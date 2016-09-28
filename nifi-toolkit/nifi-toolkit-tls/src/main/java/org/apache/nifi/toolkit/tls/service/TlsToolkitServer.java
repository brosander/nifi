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

package org.apache.nifi.toolkit.tls.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.toolkit.tls.ca.local.LocalCertificateAuthority;
import org.apache.nifi.toolkit.tls.ca.remote.server.RemoteCertificateAuthorityServer;
import org.apache.nifi.toolkit.tls.configuration.CertificateAuthorityConfig;
import org.apache.nifi.toolkit.tls.configuration.CertificateAuthorityServerConfig;
import org.apache.nifi.toolkit.tls.configuration.KeyStoreConfig;
import org.apache.nifi.toolkit.tls.configuration.RemoteConfig;
import org.apache.nifi.toolkit.tls.keystore.CertificateAuthorityKeyStoreManager;

import java.io.File;
import java.security.KeyStore;

public class TlsToolkitServer {
    private final CertificateAuthorityServerConfig certificateAuthorityServerConfig;
    private final File baseDir;
    private final ObjectMapper objectMapper;
    private final RemoteCertificateAuthorityServer remoteCertificateAuthorityServer;

    public TlsToolkitServer(File certificateAuthorityServerConfigFile) {
        this(new CertificateAuthorityServerConfig(certificateAuthorityServerConfigFile), certificateAuthorityServerConfigFile.getParentFile());
    }

    public TlsToolkitServer(CertificateAuthorityServerConfig certificateAuthorityServerConfig, File baseDir) {
        this.remoteCertificateAuthorityServer = new RemoteCertificateAuthorityServer();
        this.certificateAuthorityServerConfig = certificateAuthorityServerConfig;
        this.baseDir = baseDir;
        this.objectMapper = new ObjectMapper();
    }

    public void start() throws Exception {
        CertificateAuthorityConfig certificateAuthorityConfig = certificateAuthorityServerConfig.getCertificateAuthorityConfig();
        CertificateAuthorityKeyStoreManager certificateAuthorityKeyStoreManager = new CertificateAuthorityKeyStoreManager(certificateAuthorityConfig, baseDir);

        KeyStore keyStore = certificateAuthorityKeyStoreManager.getKeyStore();
        KeyStoreConfig keyStoreConfig = certificateAuthorityConfig.getKeyStoreConfig();
        KeyStore.PrivateKeyEntry privateKeyEntry = certificateAuthorityKeyStoreManager.getEntry(KeyStore.PrivateKeyEntry.class, keyStoreConfig.getKeyStoreAlias());

        LocalCertificateAuthority localCertificateAuthority = certificateAuthorityKeyStoreManager.getLocalCertificateAuthority();
        RemoteConfig remoteConfig = certificateAuthorityServerConfig.getRemoteConfig();

        remoteCertificateAuthorityServer.start(remoteConfig.getPort(), localCertificateAuthority, privateKeyEntry.getCertificate().getPublicKey(), keyStore, keyStoreConfig.getKeyPassword(),
                remoteConfig.getToken());
    }

    public void shutdown() throws Exception {
        remoteCertificateAuthorityServer.shutdown();
    }
}
