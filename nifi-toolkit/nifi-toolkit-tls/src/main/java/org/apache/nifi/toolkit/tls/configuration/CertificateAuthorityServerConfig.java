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

public class CertificateAuthorityServerConfig extends BaseConfiguration {
    private CertificateAuthorityConfig certificateAuthorityConfig;
    private RemoteConfig remoteConfig;

    public CertificateAuthorityServerConfig() {
    }

    public CertificateAuthorityServerConfig(File configFile) {
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public CertificateAuthorityConfig getCertificateAuthorityConfig() {
        return certificateAuthorityConfig;
    }

    public void setCertificateAuthorityConfig(CertificateAuthorityConfig certificateAuthorityConfig) {
        this.certificateAuthorityConfig = certificateAuthorityConfig;
    }

    @Override
    public void initDefaults() {
        if (remoteConfig == null) {
            remoteConfig = new RemoteConfig();
        }
        remoteConfig.initDefaults();
        if (certificateAuthorityConfig == null) {
            certificateAuthorityConfig = new CertificateAuthorityConfig();
        }
        certificateAuthorityConfig.initDefaults();
        super.initDefaults();
    }
}
