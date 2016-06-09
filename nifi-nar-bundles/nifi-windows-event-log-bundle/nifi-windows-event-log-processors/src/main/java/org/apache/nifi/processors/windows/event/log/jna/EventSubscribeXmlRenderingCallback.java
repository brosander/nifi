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
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.io.Charsets;
import org.apache.nifi.logging.ComponentLog;

import java.util.function.Consumer;

public class EventSubscribeXmlRenderingCallback implements WEvtApi.EVT_SUBSCRIBE_CALLBACK {
    public static final String RECEIVED_THE_FOLLOWING_WIN32_ERROR = "Received the following Win32 error: ";
    public static final int INITIAL_BUFFER_SIZE = 1024;

    private final ComponentLog logger;
    private final Consumer<String> consumer;
    private final int maxBufferSize;
    private final WEvtApi wEvtApi;
    private final Kernel32 kernel32;

    private int size;
    private Memory buffer;
    private Memory used;
    private Memory propertyCount;

    public EventSubscribeXmlRenderingCallback(ComponentLog logger, Consumer<String> consumer, int maxBufferSize, WEvtApi wEvtApi, Kernel32 kernel32) {
        this.logger = logger;
        this.consumer = consumer;
        this.maxBufferSize = maxBufferSize;
        this.wEvtApi = wEvtApi;
        this.kernel32 = kernel32;
        this.size = Math.min(maxBufferSize, INITIAL_BUFFER_SIZE);
        this.buffer = new Memory(size);
        this.used = new Memory(4);
        this.propertyCount = new Memory(4);
    }

    @Override
    public synchronized int onEvent(int evtSubscribeNotifyAction, WinDef.PVOID userContext, WinNT.HANDLE eventHandle) {
        if (evtSubscribeNotifyAction == WEvtApi.EvtSubscribeNotifyAction.ERROR.ordinal()) {
            logger.error(RECEIVED_THE_FOLLOWING_WIN32_ERROR + eventHandle.getPointer().getInt(0));
        } else if (evtSubscribeNotifyAction == WEvtApi.EvtSubscribeNotifyAction.DELIVER.ordinal()) {
            wEvtApi.EvtRender(null, eventHandle, WEvtApi.EvtRenderFlags.EVENT_XML.ordinal(), size, buffer, used, propertyCount);
            if (kernel32.GetLastError() == W32Errors.ERROR_INSUFFICIENT_BUFFER) {
                if (size < maxBufferSize) {
                    int newMaxSize = used.getInt(0);
                    // Check for overflow or too big
                    if (newMaxSize < size || newMaxSize > maxBufferSize) {
                        logger.error("Dropping event " + eventHandle + " because it couldn't be rendered within " + maxBufferSize + " bytes.");
                        return 0;
                    }
                    size = newMaxSize;
                    buffer = new Memory(size);
                    wEvtApi.EvtRender(null, eventHandle, WEvtApi.EvtRenderFlags.EVENT_XML.ordinal(), size, buffer, used, propertyCount);
                }
            }
            int lastError = kernel32.GetLastError();
            if (lastError == W32Errors.ERROR_SUCCESS) {
                int usedBytes = used.getInt(0);
                consumer.accept(Charsets.UTF_16LE.decode(buffer.getByteBuffer(0, usedBytes)).toString());
            } else {
                logger.error("EvtRender returned the following error code " + lastError);
            }
        }
        return 0;
    }
}
