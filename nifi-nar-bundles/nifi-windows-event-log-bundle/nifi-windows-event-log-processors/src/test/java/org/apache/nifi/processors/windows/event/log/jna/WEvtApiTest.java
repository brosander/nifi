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

package org.apache.nifi.processors.windows.event.log.jna;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.apache.commons.io.Charsets;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertNotNull;

public class WEvtApiTest {
    @Test
    public void testWevtapi() throws InterruptedException {
        Pointer subscriptionHandle = WEvtApi.INSTANCE.EvtSubscribe(null, null, "system", "*", null, null, (evtSubscribeNotifyAction, userContext, eventHandle) -> {
            int size = 1024 * 128;
            Memory buffer = new Memory(size);
            Memory used = new Memory(4);
            Memory propertyCount = new Memory(4);
            WEvtApi.INSTANCE.EvtRender(null, eventHandle, WEvtApi.EvtRenderFlags.EVENT_XML.ordinal(), size, buffer, used, propertyCount);
            int usedBytes = used.getInt(0);
            System.out.println(Charsets.UTF_16LE.decode(buffer.getByteBuffer(0, usedBytes)).toString());
            return 0;
        }, WEvtApi.EvtSubscribeFlags.START_AT_OLDEST.getValue());
        assertNotNull(subscriptionHandle);
        System.out.println(subscriptionHandle);
        while (true) {
            Thread.sleep(500);
        }
    }
}
