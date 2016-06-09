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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface WEvtApi extends StdCallLibrary {
    WEvtApi INSTANCE = (WEvtApi) Native.loadLibrary("wevtapi", WEvtApi.class, W32APIOptions.DEFAULT_OPTIONS);

    WinNT.HANDLE EvtSubscribe(WinNT.HANDLE session, WinNT.HANDLE signalEvent, String channelName, String xpathQuery, WinNT.HANDLE bookmark, WinDef.PVOID context, EVT_SUBSCRIBE_CALLBACK evtSubscribeCallback, int flags);

    boolean EvtRender(WinNT.HANDLE context, WinNT.HANDLE fragment, int flags, int bufferSize, Pointer buffer, Pointer bufferUsed, Pointer propertyCount);

    enum EvtSubscribeNotifyAction {
        ERROR, DELIVER;
    }

    enum EvtSubscribeFlags {
        SUBSCRIBE_TO_FUTURE(1), START_AT_OLDEST(2), START_AFTER_BOOKMARK(3), ORIGIN_MASK(0x3), TOLERATE_QUERY_ERRORS(0x1000), STRICT(0x10000);

        private final int value;

        EvtSubscribeFlags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    enum EvtRenderFlags {
        EVENT_VALUES, EVENT_XML, EVENT_BOOKMARK;
    }

    interface EVT_SUBSCRIBE_CALLBACK extends StdCallCallback {
        int onEvent(int evtSubscribeNotifyAction, WinDef.PVOID userContext, WinNT.HANDLE eventHandle);
    }
}
