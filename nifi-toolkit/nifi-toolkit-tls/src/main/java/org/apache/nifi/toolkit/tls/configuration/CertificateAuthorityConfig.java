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

public class CertificateAuthorityConfig extends BaseConfiguration {
    public static final int DEFAULT_DAYS = 3 * 365;

    private int days;
    private KeyStoreConfig keyStoreConfig;
    private CertificateGenerationConfig certificateGenerationConfig;

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public KeyStoreConfig getKeyStoreConfig() {
        return keyStoreConfig;
    }

    public void setKeyStoreConfig(KeyStoreConfig keyStoreConfig) {
        this.keyStoreConfig = keyStoreConfig;
    }

    public CertificateGenerationConfig getCertificateGenerationConfig() {
        return certificateGenerationConfig;
    }

    public void setCertificateGenerationConfig(CertificateGenerationConfig certificateGenerationConfig) {
        this.certificateGenerationConfig = certificateGenerationConfig;
    }

    @Override
    public void initDefaults() {
        if (days == 0) {
            days = DEFAULT_DAYS;
        }
        if (certificateGenerationConfig == null) {
            certificateGenerationConfig = new CertificateGenerationConfig();
        }
        certificateGenerationConfig.initDefaults();
        if (keyStoreConfig == null) {
            keyStoreConfig = new KeyStoreConfig();
        }
        keyStoreConfig.initDefaults();
        super.initDefaults();
    }
}
