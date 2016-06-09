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
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.Charsets;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processors.windows.event.log.jna.EventSubscribeXmlRenderingCallback;
import org.apache.nifi.processors.windows.event.log.jna.WEvtApi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JNAJUnitRunner.class)
public class EvtSubscribeTest {
    @Mock
    Kernel32 kernel32;

    @Mock
    WEvtApi wEvtApi;

    @Mock
    ProcessContext processContext;

    @Mock
    ProcessSessionFactory processSessionFactory;

    @Mock
    ProcessSession processSession;

    private EvtSubscribe evtSubscribe;

    @Before
    public void setup() {
        when(processSessionFactory.createSession()).thenReturn(processSession);
        evtSubscribe = new EvtSubscribe(wEvtApi, kernel32);
    }

    @Test
    public void testFlow() throws Exception {
        int maxEventQueue = 1;
        int maxBuffer = 1024;
        String testChannel = "testChannel";
        String testQuery = "testQuery";
        String testXml3 = "TestXml3";
        String testXml4 = "TestXml4";

        FlowFile flowFile1 = mock(FlowFile.class);
        FlowFile flowFile2 = mock(FlowFile.class);
        FlowFile flowFile3 = mock(FlowFile.class);

        FlowFile flowFile4 = mock(FlowFile.class);
        FlowFile flowFile5 = mock(FlowFile.class);
        FlowFile flowFile6 = mock(FlowFile.class);

        ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();

        when(processSession.create()).thenReturn(flowFile1).thenReturn(flowFile4).thenReturn(null);
        when(processSession.write(eq(flowFile1), isA(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(byteArrayOutputStream1);
            return flowFile2;
        });
        when(processSession.write(eq(flowFile4), isA(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(byteArrayOutputStream2);
            return flowFile5;
        });

        AtomicReference<String> mimeType1 = new AtomicReference<>(null);
        AtomicReference<String> mimeType2 = new AtomicReference<>(null);
        when(processSession.putAttribute(eq(flowFile2), eq(CoreAttributes.MIME_TYPE.key()), anyString())).thenAnswer(invocation -> {
            mimeType1.set((String) invocation.getArguments()[2]);
            return flowFile3;
        });
        when(processSession.putAttribute(eq(flowFile5), eq(CoreAttributes.MIME_TYPE.key()), anyString())).thenAnswer(invocation -> {
            mimeType2.set((String) invocation.getArguments()[2]);
            return flowFile6;
        });

        PropertyValue maxEventSize = mock(PropertyValue.class);
        when(maxEventSize.asInteger()).thenReturn(maxEventQueue);

        PropertyValue maxBufferSize = mock(PropertyValue.class);
        when(maxBufferSize.asInteger()).thenReturn(maxBuffer);

        PropertyValue channel = mock(PropertyValue.class);
        when(channel.getValue()).thenReturn(testChannel);

        PropertyValue query = mock(PropertyValue.class);
        when(query.getValue()).thenReturn(testQuery);

        WinNT.HANDLE subscriptionHandle = mock(WinNT.HANDLE.class);
        when(wEvtApi.EvtSubscribe(isNull(WinNT.HANDLE.class), isNull(WinNT.HANDLE.class), eq(testChannel), eq(testQuery),
                isNull(WinNT.HANDLE.class), isNull(WinDef.PVOID.class), isA(EventSubscribeXmlRenderingCallback.class),
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE.getValue())))
                .thenReturn(subscriptionHandle);

        when(processContext.getProperty(EvtSubscribe.MAX_EVENT_QUEUE_SIZE)).thenReturn(maxEventSize);
        when(processContext.getProperty(EvtSubscribe.MAX_BUFFER_SIZE)).thenReturn(maxBufferSize);
        when(processContext.getProperty(EvtSubscribe.CHANNEL)).thenReturn(channel);
        when(processContext.getProperty(EvtSubscribe.QUERY)).thenReturn(query);

        evtSubscribe.subscribeToEvents(processContext);
        ArgumentCaptor<EventSubscribeXmlRenderingCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(EventSubscribeXmlRenderingCallback.class);
        verify(wEvtApi).EvtSubscribe(isNull(WinNT.HANDLE.class), isNull(WinNT.HANDLE.class), eq(testChannel), eq(testQuery),
                isNull(WinNT.HANDLE.class), isNull(WinDef.PVOID.class), callbackArgumentCaptor.capture(),
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE.getValue()));

        EventSubscribeXmlRenderingCallback callback = callbackArgumentCaptor.getValue();
        Consumer<String> consumer = callback.getConsumer();
        consumer.accept("TestXml1");
        consumer.accept("TestXml2");
        consumer.accept(testXml3);

        evtSubscribe.onTrigger(processContext, processSessionFactory);
        verify(processSession).transfer(flowFile3, EvtSubscribe.REL_SUCCESS);
        verify(processSession, times(1)).transfer(any(FlowFile.class), eq(EvtSubscribe.REL_SUCCESS));

        assertEquals(processSessionFactory, evtSubscribe.getProcessSessionFactory());
        assertEquals(MediaType.APPLICATION_XML_UTF_8.toString(), mimeType1.get());
        assertEquals(testXml3, Charsets.UTF_8.decode(ByteBuffer.wrap(byteArrayOutputStream1.toByteArray())).toString());

        consumer.accept(testXml4);
        assertEquals(MediaType.APPLICATION_XML_UTF_8.toString(), mimeType2.get());
        assertEquals(testXml4, Charsets.UTF_8.decode(ByteBuffer.wrap(byteArrayOutputStream2.toByteArray())).toString());

        verify(processSession).transfer(flowFile6, EvtSubscribe.REL_SUCCESS);
        verify(processSession, times(2)).transfer(any(FlowFile.class), eq(EvtSubscribe.REL_SUCCESS));

        evtSubscribe.closeSubscriptionHandle();
        verify(kernel32).CloseHandle(subscriptionHandle);
    }

    @Test
    public void testGetSupportedPropertyDescriptors() {
        assertEquals(EvtSubscribe.PROPERTY_DESCRIPTORS, evtSubscribe.getSupportedPropertyDescriptors());
    }

    @Test
    public void testGetRelationships() {
        assertEquals(EvtSubscribe.RELATIONSHIPS, evtSubscribe.getRelationships());
    }
}
