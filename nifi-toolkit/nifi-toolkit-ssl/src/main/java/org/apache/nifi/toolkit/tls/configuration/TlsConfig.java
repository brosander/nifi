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

import java.util.Collections;
import java.util.Map;

public class TlsConfig {
    public static final String NIFI_TOOLKIT_TLS_KEY_STORE = "nifi.toolkit.tls.keyStore";
    public static final String NIFI_TOOLKIT_TLS_KEY_STORE_TYPE = "nifi.toolkit.tls.keyStoreType";
    public static final String NIFI_TOOLKIT_TLS_KEY_STORE_PASSWORD = "nifi.toolkit.tls.keyStorePassword";
    public static final String NIFI_TOOLKIT_TLS_KEY_PASSWORD = "nifi.toolkit.tls.keyPassword";
    public static final String NIFI_TOOLKIT_TLS_TOKEN = "nifi.toolkit.tls.token";
    public static final String NIFI_TOOLKIT_TLS_HOSTNAME = "nifi.toolkit.tls.hostname";
    public static final String NIFI_TOOLKIT_TLS_PORT = "nifi.toolkit.tls.port";
    public static final String DEFAULT_PORT = "8443";
    private TlsHelperConfig tlsHelperConfig;
    private String keyStore;
    private String keyStoreType;
    private String keyStorePassword;
    private String keyPassword;
    private String token;
    private String hostname;
    private int port;

    public TlsConfig() {
        this(Collections.emptyMap());
    }

    public TlsConfig(Map<String, String> map) {
        keyStore = map.get(NIFI_TOOLKIT_TLS_KEY_STORE);
        keyStoreType = map.get(NIFI_TOOLKIT_TLS_KEY_STORE_TYPE);
        keyStorePassword = map.get(NIFI_TOOLKIT_TLS_KEY_STORE_PASSWORD);
        keyPassword = map.get(NIFI_TOOLKIT_TLS_KEY_PASSWORD);
        token = map.get(NIFI_TOOLKIT_TLS_TOKEN);
        hostname = map.get(NIFI_TOOLKIT_TLS_HOSTNAME);
        String portString = map.get(NIFI_TOOLKIT_TLS_PORT);
        if (StringUtils.isEmpty(portString)) {
            portString = DEFAULT_PORT;
        }
        port = Integer.parseInt(portString);
        tlsHelperConfig = new TlsHelperConfig(map);
    }

    public void save(Map<String, String> map) {
        map.put(NIFI_TOOLKIT_TLS_KEY_STORE, keyStore);
        map.put(NIFI_TOOLKIT_TLS_KEY_STORE_TYPE, keyStoreType);
        map.put(NIFI_TOOLKIT_TLS_KEY_STORE_PASSWORD, keyStorePassword);
        map.put(NIFI_TOOLKIT_TLS_KEY_PASSWORD, keyPassword);
        map.put(NIFI_TOOLKIT_TLS_TOKEN, token);
        map.put(NIFI_TOOLKIT_TLS_HOSTNAME, hostname);
        map.put(NIFI_TOOLKIT_TLS_PORT, Integer.toString(port));
        tlsHelperConfig.save(map);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public TlsHelperConfig getTlsHelperConfig() {
        return tlsHelperConfig;
    }

    public void setTlsHelperConfig(TlsHelperConfig tlsHelperConfig) {
        this.tlsHelperConfig = tlsHelperConfig;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
