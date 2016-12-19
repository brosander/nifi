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

package org.apache.nifi.processor;

import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.exception.ProcessException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractBatchingProcessorTest {
    @Mock
    ProcessSessionFactory processSessionFactory;

    @Mock
    ProcessSession processSession;

    @Mock
    ProcessContext processContext;

    private AbstractBatchingProcessor<Object> abstractBatchingProcessor;
    private Object invariantObject;
    private List<FlowFile> flowFiles;

    @Before
    public void setup() {
        invariantObject = new Object();
        when(processSessionFactory.createSession()).thenReturn(processSession);
        abstractBatchingProcessor = mock(ConcreteBatchingProcessor.class);
        when(abstractBatchingProcessor.getOnTriggerInvariants(processContext)).thenReturn(invariantObject);
        flowFiles = IntStream.range(0, 50).mapToObj(i -> mock(FlowFile.class)).collect(Collectors.toList());
        doCallRealMethod().when(abstractBatchingProcessor).getBatchSize(processContext);
    }

    private void testOnTrigger() {
        PropertyValue propertyValue = mock(PropertyValue.class);

        when(propertyValue.asInteger()).thenReturn(flowFiles.size());
        when(processContext.getProperty(AbstractBatchingProcessor.BATCH_SIZE)).thenReturn(propertyValue);
        when(processSession.get(flowFiles.size())).thenReturn(flowFiles);

        abstractBatchingProcessor.getBatchSize(processContext);
        abstractBatchingProcessor.onTrigger(processContext, processSessionFactory);

        flowFiles.forEach(f -> verify(abstractBatchingProcessor).onTrigger(f, processContext, processSession, invariantObject));
        verify(abstractBatchingProcessor, times(1)).getOnTriggerInvariants(processContext);
        verify(abstractBatchingProcessor, times(flowFiles.size())).onTrigger(any(FlowFile.class), eq(processContext), eq(processSession), eq(invariantObject));
        verify(processSession, times(1)).commit();
    }

    @Test
    public void testOnTrigger50FlowFiles() {
        testOnTrigger();
    }

    @Test
    public void testOnTrigger0FlowFiles() {
        flowFiles.clear();
        testOnTrigger();
    }

    @Test
    public void testOnTrigger1lowFiles() {
        flowFiles = flowFiles.subList(0, 1);
        testOnTrigger();
    }

    private static class ConcreteBatchingProcessor extends AbstractBatchingProcessor<Object> {

        @Override
        protected void onTrigger(FlowFile flowFile, ProcessContext context, ProcessSession session, Object invariants) throws ProcessException {

        }
    }
}
