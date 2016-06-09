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

import com.google.common.net.MediaType;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.Charsets;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class EvtSubscribe extends AbstractSessionFactoryProcessor {
    public static final PropertyDescriptor CHANNEL = new PropertyDescriptor.Builder()
            .name("channel")
            .displayName("Channel")
            .required(true)
            .defaultValue("System")
            .description("The Windows Event Log Channel to listen to")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor QUERY = new PropertyDescriptor.Builder()
            .name("query")
            .displayName("XPath Query")
            .required(true)
            .defaultValue("*")
            .description("XPath Query to filter events (See https://msdn.microsoft.com/en-us/library/windows/desktop/dd996910(v=vs.85).aspx for examples)")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MAX_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("maxBuffer")
            .displayName("Maximum Buffer Size")
            .required(true)
            .defaultValue(Integer.toString(1024 * 1024))
            .description("The individual Event Log XMLs are rendered to a buffer.  This specifies the maximum size in bytes that the buffer will be allowed to grow to.")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor MAX_EVENT_QUEUE_SIZE = new PropertyDescriptor.Builder()
            .name("maxQueue")
            .displayName("Maximum queue size")
            .defaultValue("1024")
            .description("Maximum number of events to Queue for transformation into FlowFiles before the Processor starts running")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder().build();

    private final AtomicReference<ProcessSessionFactory> sessionFactoryReference;
    private final Queue<String> renderedXmls;
    private final WEvtApi wEvtApi;
    private final Kernel32 kernel32;

    private WEvtApi.EVT_SUBSCRIBE_CALLBACK evtSubscribeCallback;
    private WinNT.HANDLE subscriptionHandle;

    public EvtSubscribe() {
        this(WEvtApi.INSTANCE, Kernel32.INSTANCE);
    }

    public EvtSubscribe(WEvtApi wEvtApi, Kernel32 kernel32) {
        this.wEvtApi = wEvtApi;
        this.kernel32 = kernel32;
        this.sessionFactoryReference = new AtomicReference<>();
        this.renderedXmls = new LinkedList<>();
    }

    @OnScheduled
    public void subscribeToEvents(ProcessContext context) throws Exception {
        PropertyValue maxEventQueueSize = context.getProperty(MAX_EVENT_QUEUE_SIZE);
        evtSubscribeCallback = new EventSubscribeXmlRenderingCallback(getLogger(), s -> {
            ProcessSessionFactory processSessionFactory = sessionFactoryReference.get();
            if (processSessionFactory == null) {
                renderedXmls.add(s);
                while (renderedXmls.size() > maxEventQueueSize.asInteger()) {
                    renderedXmls.poll();
                }
            } else {
                createAndTransferEventFlowFile(processSessionFactory.createSession(), s);
            }
        }, context.getProperty(MAX_BUFFER_SIZE).asInteger(), wEvtApi, kernel32);
        subscriptionHandle = wEvtApi.EvtSubscribe(null, null,
                context.getProperty(CHANNEL).getValue(), context.getProperty(QUERY).getValue(), null, null,
                evtSubscribeCallback, WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE.getValue());
    }

    @OnStopped
    public void closeSubscriptionHandle() {
        kernel32.CloseHandle(subscriptionHandle);
        subscriptionHandle = null;
    }

    protected void createAndTransferEventFlowFile(ProcessSession session, String xml) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, out -> out.write(xml.getBytes(Charsets.UTF_8)));
        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        session.transfer(flowFile, REL_SUCCESS);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSessionFactory sessionFactory) throws ProcessException {
        ProcessSession session = sessionFactory.createSession();
        synchronized (evtSubscribeCallback) {
            sessionFactoryReference.compareAndSet(null, sessionFactory);
            String renderedXml;
            while ((renderedXml = renderedXmls.poll()) != null) {
                createAndTransferEventFlowFile(session, renderedXml);
            }
        }
    }
}
