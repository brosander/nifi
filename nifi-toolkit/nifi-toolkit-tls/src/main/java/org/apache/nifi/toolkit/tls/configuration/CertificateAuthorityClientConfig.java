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

import java.io.File;

public class CertificateAuthorityClientConfig extends BaseConfiguration {
    public static final String DEFAULT_TRUST_STORE_ALIAS = "nifi-cert";
    private CertificateGenerationConfig certificateGenerationConfig;
    private KeyStoreConfig keyStoreConfig;
    private KeyStoreConfig trustStoreConfig;

    public CertificateAuthorityClientConfig() {
    }

    public CertificateAuthorityClientConfig(File file) {

    }

    public CertificateAuthorityClientConfig(StandaloneConfig standaloneConfig, InstanceDefinition instanceDefinition, File baseDir) {
        certificateGenerationConfig = new CertificateGenerationConfig(standaloneConfig.getCertificateAuthorityConfig().getCertificateGenerationConfig());
        initDefaults();
        keyStoreConfig.setKeyStore(new File(baseDir, "keystore." + keyStoreConfig.getKeyStoreType()).getAbsolutePath());
        keyStoreConfig.setKeyStorePassword(instanceDefinition.getKeyStorePassword());
        keyStoreConfig.setKeyPassword(instanceDefinition.getKeyPassword());
        trustStoreConfig.setKeyStore(new File(baseDir, "truststore." + keyStoreConfig.getKeyStoreType()).getAbsolutePath());
        trustStoreConfig.setKeyStorePassword(instanceDefinition.getTrustStorePassword());
    }

    public KeyStoreConfig getKeyStoreConfig() {
        return keyStoreConfig;
    }

    public void setKeyStoreConfig(KeyStoreConfig keyStoreConfig) {
        this.keyStoreConfig = keyStoreConfig;
    }

    public KeyStoreConfig getTrustStoreConfig() {
        return trustStoreConfig;
    }

    public void setTrustStoreConfig(KeyStoreConfig trustStoreConfig) {
        this.trustStoreConfig = trustStoreConfig;
    }

    public CertificateGenerationConfig getCertificateGenerationConfig() {
        return certificateGenerationConfig;
    }

    public void setCertificateGenerationConfig(CertificateGenerationConfig certificateGenerationConfig) {
        this.certificateGenerationConfig = certificateGenerationConfig;
    }

    @Override
    public void initDefaults() {
        if (keyStoreConfig == null) {
            keyStoreConfig = new KeyStoreConfig();
        }
        keyStoreConfig.initDefaults();
        if (trustStoreConfig == null) {
            trustStoreConfig = new KeyStoreConfig();
            trustStoreConfig.setKeyStoreAlias(DEFAULT_TRUST_STORE_ALIAS);
        }
        trustStoreConfig.initDefaults();
        if (certificateGenerationConfig == null) {
            certificateGenerationConfig = new CertificateGenerationConfig();
        }
        certificateGenerationConfig.initDefaults();
        super.initDefaults();
    }
}
