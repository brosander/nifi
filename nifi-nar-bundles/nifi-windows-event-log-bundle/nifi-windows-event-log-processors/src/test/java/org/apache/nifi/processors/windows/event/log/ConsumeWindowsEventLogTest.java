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

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.Charsets;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processors.windows.event.log.jna.EventSubscribeXmlRenderingCallback;
import org.apache.nifi.processors.windows.event.log.jna.WEvtApi;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.ReflectionUtils;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JNAJUnitRunner.class)
public class ConsumeWindowsEventLogTest {
    @Mock
    Kernel32 kernel32;

    @Mock
    WEvtApi wEvtApi;

    private ConsumeWindowsEventLog evtSubscribe;
    private TestRunner testRunner;
    private WinNT.HANDLE subscriptionHandle;

    public static List<WinNT.HANDLE> mockEventHandles(WEvtApi wEvtApi, Kernel32 kernel32, List<String> eventXmls) {
        List<WinNT.HANDLE> eventHandles = new ArrayList<>();
        for (String eventXml : eventXmls) {
            WinNT.HANDLE eventHandle = mock(WinNT.HANDLE.class);
            when(wEvtApi.EvtRender(isNull(WinNT.HANDLE.class), eq(eventHandle), eq(WEvtApi.EvtRenderFlags.EVENT_XML),
                    anyInt(), any(Pointer.class), any(Pointer.class), any(Pointer.class))).thenAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                Pointer bufferUsed = (Pointer) arguments[5];
                byte[] array = Charsets.UTF_16LE.encode(eventXml).array();
                if (array.length > (int) arguments[3]) {
                    when(kernel32.GetLastError()).thenReturn(W32Errors.ERROR_INSUFFICIENT_BUFFER).thenReturn(W32Errors.ERROR_SUCCESS);
                } else {
                    ((Pointer) arguments[4]).write(0, array, 0, array.length);
                }
                bufferUsed.setInt(0, array.length);
                return false;
            });
            eventHandles.add(eventHandle);
        }
        return eventHandles;
    }

    /*@Test
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
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE)))
                .thenReturn(subscriptionHandle);

        when(processContext.getProperty(ConsumeWindowsEventLog.MAX_EVENT_QUEUE_SIZE)).thenReturn(maxEventSize);
        when(processContext.getProperty(ConsumeWindowsEventLog.MAX_BUFFER_SIZE)).thenReturn(maxBufferSize);
        when(processContext.getProperty(ConsumeWindowsEventLog.CHANNEL)).thenReturn(channel);
        when(processContext.getProperty(ConsumeWindowsEventLog.QUERY)).thenReturn(query);

        evtSubscribe.subscribeToEvents(processContext);
        ArgumentCaptor<EventSubscribeXmlRenderingCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(EventSubscribeXmlRenderingCallback.class);
        verify(wEvtApi).EvtSubscribe(isNull(WinNT.HANDLE.class), isNull(WinNT.HANDLE.class), eq(testChannel), eq(testQuery),
                isNull(WinNT.HANDLE.class), isNull(WinDef.PVOID.class), callbackArgumentCaptor.capture(),
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE));

        EventSubscribeXmlRenderingCallback callback = callbackArgumentCaptor.getValue();
        Consumer<String> consumer = callback.getConsumer();
        consumer.accept("TestXml1");
        consumer.accept("TestXml2");
        consumer.accept(testXml3);

        evtSubscribe.onTrigger(processContext, processSessionFactory);
        verify(processSession).transfer(flowFile3, ConsumeWindowsEventLog.REL_SUCCESS);
        verify(processSession, times(1)).transfer(any(FlowFile.class), eq(ConsumeWindowsEventLog.REL_SUCCESS));
        verify(processContext).yield();

        assertEquals(processSessionFactory, evtSubscribe.getProcessSessionFactory());
        assertEquals(ConsumeWindowsEventLog.APPLICATION_XML, mimeType1.get());
        assertEquals(testXml3, Charsets.UTF_8.decode(ByteBuffer.wrap(byteArrayOutputStream1.toByteArray())).toString());

        consumer.accept(testXml4);
        assertEquals(ConsumeWindowsEventLog.APPLICATION_XML, mimeType2.get());
        assertEquals(testXml4, Charsets.UTF_8.decode(ByteBuffer.wrap(byteArrayOutputStream2.toByteArray())).toString());

        verify(processSession).transfer(flowFile6, ConsumeWindowsEventLog.REL_SUCCESS);
        verify(processSession, times(2)).transfer(any(FlowFile.class), eq(ConsumeWindowsEventLog.REL_SUCCESS));

        evtSubscribe.closeSubscriptionHandle();
        verify(kernel32).CloseHandle(subscriptionHandle);
    }*/

    @Before
    public void setup() {
        evtSubscribe = new ConsumeWindowsEventLog(wEvtApi, kernel32);

        subscriptionHandle = mock(WinNT.HANDLE.class);
        when(wEvtApi.EvtSubscribe(isNull(WinNT.HANDLE.class), isNull(WinNT.HANDLE.class), eq(ConsumeWindowsEventLog.DEFAULT_CHANNEL), eq(ConsumeWindowsEventLog.DEFAULT_XPATH),
                isNull(WinNT.HANDLE.class), isNull(WinDef.PVOID.class), isA(EventSubscribeXmlRenderingCallback.class),
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE)))
                .thenReturn(subscriptionHandle);

        testRunner = TestRunners.newTestRunner(evtSubscribe);
    }

    @Test(timeout = 10 * 1000)
    public void testProcessesBlockedEvents() throws UnsupportedEncodingException {
        testRunner.setProperty(ConsumeWindowsEventLog.MAX_EVENT_QUEUE_SIZE, "1");
        testRunner.run(1, false, true);
        EventSubscribeXmlRenderingCallback renderingCallback = getRenderingCallback();

        List<String> eventXmls = Arrays.asList("one", "two", "three");
        List<WinNT.HANDLE> eventHandles = mockEventHandles(wEvtApi, kernel32, eventXmls);
        AtomicBoolean done = new AtomicBoolean(false);
        new Thread(() -> {
            for (WinNT.HANDLE eventHandle : eventHandles) {
                renderingCallback.onEvent(WEvtApi.EvtSubscribeNotifyAction.DELIVER, null, eventHandle);
            }
            done.set(true);
        }).start();

        // Wait until the thread has really started
        while (testRunner.getFlowFilesForRelationship(ConsumeWindowsEventLog.REL_SUCCESS).size() == 0) {
            testRunner.run(1, false, false);
        }

        // Should at least be blocked trying to add 3rd event
        assertFalse(done.get());

        // Process rest of events
        while (!done.get()) {
            testRunner.run(1, false, false);
        }

        testRunner.run(1, true, false);

        List<MockFlowFile> flowFilesForRelationship = testRunner.getFlowFilesForRelationship(ConsumeWindowsEventLog.REL_SUCCESS);
        assertEquals(eventXmls.size(), flowFilesForRelationship.size());
        for (int i = 0; i < eventXmls.size(); i++) {
            flowFilesForRelationship.get(i).assertContentEquals(eventXmls.get(i));
        }
    }

    @Test
    public void testStopProcessesQueue() throws InvocationTargetException, IllegalAccessException {
        testRunner.run(1, false);

        List<String> eventXmls = Arrays.asList("one", "two", "three");
        for (WinNT.HANDLE eventHandle : mockEventHandles(wEvtApi, kernel32, eventXmls)) {
            getRenderingCallback().onEvent(WEvtApi.EvtSubscribeNotifyAction.DELIVER, null, eventHandle);
        }

        ReflectionUtils.invokeMethodsWithAnnotation(OnStopped.class, evtSubscribe, testRunner.getProcessContext());

        List<MockFlowFile> flowFilesForRelationship = testRunner.getFlowFilesForRelationship(ConsumeWindowsEventLog.REL_SUCCESS);
        assertEquals(eventXmls.size(), flowFilesForRelationship.size());
        for (int i = 0; i < eventXmls.size(); i++) {
            flowFilesForRelationship.get(i).assertContentEquals(eventXmls.get(i));
        }
    }

    @Test
    public void testStopClosesHandle() {
        testRunner.run(1);
        verify(kernel32).CloseHandle(subscriptionHandle);
    }

    @Test(expected = ProcessException.class)
    public void testScheduleQueueStopThrowsException() throws Throwable {
        ReflectionUtils.invokeMethodsWithAnnotation(OnScheduled.class, evtSubscribe, testRunner.getProcessContext());

        WinNT.HANDLE handle = mockEventHandles(wEvtApi, kernel32, Arrays.asList("test")).get(0);
        getRenderingCallback().onEvent(WEvtApi.EvtSubscribeNotifyAction.DELIVER, null, handle);

        try {
            ReflectionUtils.invokeMethodsWithAnnotation(OnStopped.class, evtSubscribe, testRunner.getProcessContext());
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public EventSubscribeXmlRenderingCallback getRenderingCallback() {
        ArgumentCaptor<EventSubscribeXmlRenderingCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(EventSubscribeXmlRenderingCallback.class);
        verify(wEvtApi).EvtSubscribe(isNull(WinNT.HANDLE.class), isNull(WinNT.HANDLE.class), eq(ConsumeWindowsEventLog.DEFAULT_CHANNEL), eq(ConsumeWindowsEventLog.DEFAULT_XPATH),
                isNull(WinNT.HANDLE.class), isNull(WinDef.PVOID.class), callbackArgumentCaptor.capture(),
                eq(WEvtApi.EvtSubscribeFlags.SUBSCRIBE_TO_FUTURE));
        return callbackArgumentCaptor.getValue();
    }

    @Test
    public void testGetSupportedPropertyDescriptors() {
        assertEquals(ConsumeWindowsEventLog.PROPERTY_DESCRIPTORS, evtSubscribe.getSupportedPropertyDescriptors());
    }

    @Test
    public void testGetRelationships() {
        assertEquals(ConsumeWindowsEventLog.RELATIONSHIPS, evtSubscribe.getRelationships());
    }
}
