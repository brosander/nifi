/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nifi.processors.windows.event.log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.MediaType;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.Charsets;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractSessionFactoryProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.windows.event.log.jna.EventSubscribeXmlRenderingCallback;
import org.apache.nifi.processors.windows.event.log.jna.WEvtApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@Tags({"ingest", "event", "windows"})
@CapabilityDescription("Registers a Windows Event Log Subscribe Callback to receive FlowFiles from Events on Windows.  These can be filtered via channel and xpath.")
@WritesAttributes({
        @WritesAttribute(attribute = "mime.type", description = "Will set a MIME type value of application/xml")
})
public class EvtSubscribe extends AbstractSessionFactoryProcessor {
    public static final String DEFAULT_CHANNEL = "System";
    public static final String DEFAULT_XPATH = "*";
    public static final int DEFAULT_MAX_BUFFER = 1024 * 1024;
    public static final int DEFAULT_MAX_QUEUE_SIZE = 1024;

    public static final PropertyDescriptor CHANNEL = new PropertyDescriptor.Builder()
            .name("channel")
            .displayName("Channel")
            .required(true)
            .defaultValue(DEFAULT_CHANNEL)
            .description("The Windows Event Log Channel to listen to")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor QUERY = new PropertyDescriptor.Builder()
            .name("query")
            .displayName("XPath Query")
            .required(true)
            .defaultValue(DEFAULT_XPATH)
            .description("XPath Query to filter events (See https://msdn.microsoft.com/en-us/library/windows/desktop/dd996910(v=vs.85).aspx for examples)")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MAX_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("maxBuffer")
            .displayName("Maximum Buffer Size")
            .required(true)
            .defaultValue(Integer.toString(DEFAULT_MAX_BUFFER))
            .description("The individual Event Log XMLs are rendered to a buffer.  This specifies the maximum size in bytes that the buffer will be allowed to grow to.")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor MAX_EVENT_QUEUE_SIZE = new PropertyDescriptor.Builder()
            .name("maxQueue")
            .displayName("Maximum queue size")
            .defaultValue(Integer.toString(DEFAULT_MAX_QUEUE_SIZE))
            .description("Maximum number of events to Queue for transformation into FlowFiles before the Processor starts running")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(CHANNEL, QUERY, MAX_BUFFER_SIZE, MAX_EVENT_QUEUE_SIZE));

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Relationship for successfully formatted events")
            .build();

    public static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REL_SUCCESS)));

    private final AtomicReference<ProcessSessionFactory> sessionFactoryReference;
    private final Queue<String> renderedXMLs;
    private final WEvtApi wEvtApi;
    private final Kernel32 kernel32;

    private UnsatisfiedLinkError wEvtApiError = null;
    private UnsatisfiedLinkError kernel32Error = null;

    private WEvtApi.EVT_SUBSCRIBE_CALLBACK evtSubscribeCallback;
    private WinNT.HANDLE subscriptionHandle;

    private WEvtApi loadWEvtApi() {
        try {
            return WEvtApi.INSTANCE;
        } catch (UnsatisfiedLinkError e) {
            wEvtApiError = e;
            return null;
        }
    }

    private Kernel32 loadKernel32() {
        try {
            return Kernel32.INSTANCE;
        } catch (UnsatisfiedLinkError e) {
            kernel32Error = e;
            return null;
        }
    }

    /**
     * Framework constructor
     */
    public EvtSubscribe() {
        this(null, null);
    }

    /**
     * Constructor that allows injection of JNA interfaces
     *
     * @param wEvtApi event api interface
     * @param kernel32 kernel interface
     */
    public EvtSubscribe(WEvtApi wEvtApi, Kernel32 kernel32) {
        this.wEvtApi = wEvtApi == null ? loadWEvtApi() : wEvtApi;
        this.kernel32 = kernel32 == null ? loadKernel32() : kernel32;
        this.sessionFactoryReference = new AtomicReference<>();
        this.renderedXMLs = new LinkedList<>();
    }

    /**
     * Register subscriber via native call
     *
     * @param context the process context
     */
    @OnScheduled
    public void subscribeToEvents(ProcessContext context) {
        int maxEventQueueSize = context.getProperty(MAX_EVENT_QUEUE_SIZE).asInteger();
        evtSubscribeCallback = new EventSubscribeXmlRenderingCallback(getLogger(), s -> {
            ProcessSessionFactory processSessionFactory = sessionFactoryReference.get();
            if (processSessionFactory == null) {
                addRenderedXml(s, maxEventQueueSize);
            } else {
                createAndTransferEventFlowFile(processSessionFactory.createSession(), s);
            }
        }, context.getProperty(MAX_BUFFER_SIZE).asInteger(), wEvtApi, kernel32);
        subscriptionHandle = wEvtApi.EvtSubscribe(null, null,
                context.getProperty(CHANNEL).getValue(), context.getProperty(QUERY).getValue(), null, null,
                evtSubscribeCallback, WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE);
    }

    /**
     * Adds a rendered xml to the queue to be processed.
     * Should only be called within block synchronized on evtSubscribeCallback before the sessionFactoryReference is set
     *
     * @param s the rendered xml string
     * @param maxEventQueueSize the maximum size of the event queue
     */
    protected void addRenderedXml(String s, int maxEventQueueSize) {
        renderedXMLs.add(s);
        while (renderedXMLs.size() > maxEventQueueSize) {
            renderedXMLs.poll();
        }
    }

    /**
     * Cleanup native subscription
     */
    @OnStopped
    public void closeSubscriptionHandle() {
        kernel32.CloseHandle(subscriptionHandle);
        subscriptionHandle = null;
    }

    /**
     * Creates a flow file from the given XML and transfers it to the success relationship
     *
     * @param session the process session
     * @param xml the XML
     */
    protected void createAndTransferEventFlowFile(ProcessSession session, String xml) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, out -> out.write(xml.getBytes(Charsets.UTF_8)));
        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        session.transfer(flowFile, REL_SUCCESS);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSessionFactory sessionFactory) throws ProcessException {
        ProcessSession session = sessionFactory.createSession();

        // Give callback the information it needs to do its job and empty queue of events from before trigger
        synchronized (evtSubscribeCallback) {
            sessionFactoryReference.compareAndSet(null, sessionFactory);
            String renderedXml;
            while ((renderedXml = renderedXMLs.poll()) != null) {
                createAndTransferEventFlowFile(session, renderedXml);
            }
        }
        context.yield();
    }

    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        // We need to check to see if the native libraries loaded properly
        List<ValidationResult> validationResults = new ArrayList<>(super.customValidate(validationContext));
        if (wEvtApiError != null) {
            validationResults.add(new ValidationResult.Builder().valid(false)
                    .explanation("Unable to load wevtapi on this system.  This processor utilizes native Windows APIs and will only work on Windows. (" + wEvtApiError.getMessage() + ")").build());
        }
        if (kernel32Error != null) {
            validationResults.add(new ValidationResult.Builder().valid(false)
                    .explanation("Unable to load kernel32 on this system.  This processor utilizes native Windows APIs and will only work on Windows. (" + kernel32Error.getMessage() + ")").build());
        }
        return validationResults;
    }

    @VisibleForTesting
    protected ProcessSessionFactory getProcessSessionFactory() {
        return sessionFactoryReference.get();
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }
}
