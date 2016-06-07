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

/**
 * JNA will create the instance of this interface with Native.loadLibrary().
 * Please see https://msdn.microsoft.com/en-us/library/windows/desktop/aa385772(v=vs.85).aspx for documentation on the methods and data structures.
 */
public interface WEvtApi extends StdCallLibrary {
    WEvtApi INSTANCE = (WEvtApi) Native.loadLibrary("wevtapi", WEvtApi.class, W32APIOptions.DEFAULT_OPTIONS);

    WinNT.HANDLE EvtSubscribe(WinNT.HANDLE session, WinNT.HANDLE signalEvent, String channelName, String xpathQuery,
                              WinNT.HANDLE bookmark, WinDef.PVOID context, EVT_SUBSCRIBE_CALLBACK evtSubscribeCallback, int flags);

    boolean EvtRender(WinNT.HANDLE context, WinNT.HANDLE fragment, int flags, int bufferSize, Pointer buffer, Pointer bufferUsed, Pointer propertyCount);

    interface EvtSubscribeNotifyAction {
        int ERROR = 0;
        int DELIVER = 1;
    }

    interface EvtSubscribeFlags {
        int SUBSCRIBE_TO_FUTURE = 1;
        int START_AT_OLDEST = 2;
        int START_AFTER_BOOKMARK = 3;
        int ORIGIN_MASK = 0x3;
        int TOLERATE_QUERY_ERRORS = 0x1000;
        int STRICT = 0x10000;
    }

    interface EvtRenderFlags {
        int EVENT_VALUES = 0;
        int EVENT_XML = 1;
        int EVENT_BOOKMARK = 2;
    }

    interface EVT_SUBSCRIBE_CALLBACK extends StdCallCallback {
        int onEvent(int evtSubscribeNotifyAction, WinDef.PVOID userContext, WinNT.HANDLE eventHandle);
    }
}
