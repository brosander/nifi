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

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.exception.ProcessException;

public abstract class AbstractBatchingProcessor<T> extends AbstractProcessor {
    public static final PropertyDescriptor BATCH_SIZE = new PropertyDescriptor.Builder()
            .name(AbstractBatchingProcessor.class.getCanonicalName() + ".batchSize")
            .displayName("Batch Size")
            .description("Specifies how many flow files are requested from the session at a time.")
            .required(true)
            .addValidator((subject, input, context) -> {
                String reason = null;
                try {
                    Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    reason = "not a valid integer";
                }
                return new ValidationResult.Builder().subject(subject).input(input).explanation(reason).valid(reason == null).build();
            })
            .defaultValue(Integer.toString(50))
            .build();

    private int batchSize;

    @OnScheduled
    public void getBatchSize(ProcessContext processContext) {
        batchSize = processContext.getProperty(BATCH_SIZE).asInteger();
    }

    @Override
    public final void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        T invariants = getOnTriggerInvariants(context);
        for (FlowFile flowFile : session.get(batchSize)) {
            doOnTrigger(flowFile, context, session, invariants);
        }
    }

    /**
     * Method that can be overridden to return a container object for things that should be the same for the entire batch
     *
     * @param processContext the process context
     * @return a container object for things that should be the same for the entire batch
     */
    protected T getOnTriggerInvariants(ProcessContext processContext) {
        return null;
    }

    /**
     * Will be called for each FlowFile in the batch
     * @param flowFile the FlowFile
     * @param context the ProcessContext (any properties that can be should be fetched in getOnTriggerInvariants()
     * @param session the session
     * @param invariants the result of calling getOnTriggerInvariants() with the ProcessContext
     *
     * @throws ProcessException if processing did not complete normally though
     * indicates the problem is an understood potential outcome of processing.
     */
    protected abstract void doOnTrigger(FlowFile flowFile, ProcessContext context, ProcessSession session, T invariants) throws ProcessException;
}
