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

package org.apache.nifi.toolkit.tls.configuration;

import org.apache.nifi.util.StringUtils;

public class RemoteConfig extends BaseConfiguration{
    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 8443;

    private String hostname = DEFAULT_HOSTNAME;
    private String token;
    private int port = DEFAULT_PORT;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void initDefaults() {
        if (port == 0) {
            port = DEFAULT_PORT;
        }
        if (StringUtils.isEmpty(hostname)) {
            hostname = DEFAULT_HOSTNAME;
        }
        super.initDefaults();
    }
}
