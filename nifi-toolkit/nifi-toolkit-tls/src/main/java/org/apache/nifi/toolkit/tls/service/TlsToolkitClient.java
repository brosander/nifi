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

import org.apache.nifi.toolkit.tls.ca.CertificateAuthority;
import org.apache.nifi.toolkit.tls.ca.remote.client.RemoteCertificateAuthorityClient;
import org.apache.nifi.toolkit.tls.configuration.CertificateAuthorityClientConfig;
import org.apache.nifi.toolkit.tls.configuration.CertificateGenerationConfig;
import org.apache.nifi.toolkit.tls.configuration.KeyStoreConfig;
import org.apache.nifi.toolkit.tls.configuration.RemoteConfig;
import org.apache.nifi.toolkit.tls.configuration.writer.ConfigurationWriter;
import org.apache.nifi.toolkit.tls.keystore.KeyStoreManager;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.TlsHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

public class TlsToolkitClient {
    private final CertificateAuthority certificateAuthority;
    private final OutputStreamFactory outputStreamFactory;

    public TlsToolkitClient(RemoteConfig remoteConfig) {
        this(new RemoteCertificateAuthorityClient(remoteConfig.getHostname(), remoteConfig.getPort(), remoteConfig.getToken()));
    }

    public TlsToolkitClient(CertificateAuthority certificateAuthority) {
        this(certificateAuthority, FileOutputStream::new);
    }

    public TlsToolkitClient(CertificateAuthority certificateAuthority, OutputStreamFactory outputStreamFactory) {
        this.certificateAuthority = certificateAuthority;
        this.outputStreamFactory = outputStreamFactory;
    }

    public void getCertificates(File certificateAuthorityClientConfigFile, List<ConfigurationWriter<CertificateAuthorityClientConfig>> configurationWriters)
            throws GeneralSecurityException, IOException {
        getCertificates(new CertificateAuthorityClientConfig(certificateAuthorityClientConfigFile), certificateAuthorityClientConfigFile.getParentFile(), configurationWriters);
    }

    public void getCertificates(CertificateAuthorityClientConfig certificateAuthorityClientConfig, File baseDir,
                                List<ConfigurationWriter<CertificateAuthorityClientConfig>> configurationWriters) throws GeneralSecurityException, IOException {
        CertificateGenerationConfig certificateGenerationConfig = certificateAuthorityClientConfig.getCertificateGenerationConfig();

        KeyPair keyPair = TlsHelper.generateKeyPair(certificateGenerationConfig.getKeyPairAlgorithm(), certificateGenerationConfig.getKeySize());
        X509Certificate[] certificateChain = certificateAuthority.sign(TlsHelper.generateCertificationRequest(certificateGenerationConfig.getDn(), keyPair,
                certificateGenerationConfig.getSigningAlgorithm()));

        KeyStoreConfig keyStoreConfig = certificateAuthorityClientConfig.getKeyStoreConfig();
        KeyStoreManager keyStoreManager = new KeyStoreManager(keyStoreConfig, baseDir);
        keyStoreManager.addPrivateKeyToKeyStore(keyPair, keyStoreConfig.getKeyStoreAlias(), certificateChain);
        keyStoreManager.storeKeystore();

        KeyStoreConfig trustStoreConfig = certificateAuthorityClientConfig.getTrustStoreConfig();
        KeyStoreManager trustStoreManager = new KeyStoreManager(trustStoreConfig, baseDir);
        trustStoreManager.getKeyStore().setCertificateEntry(trustStoreConfig.getKeyStoreAlias(), certificateChain[certificateChain.length - 1]);
        trustStoreManager.storeKeystore();

        for (ConfigurationWriter<CertificateAuthorityClientConfig> configurationWriter : configurationWriters) {
            configurationWriter.write(certificateAuthorityClientConfig, baseDir, outputStreamFactory);
        }
    }
}
