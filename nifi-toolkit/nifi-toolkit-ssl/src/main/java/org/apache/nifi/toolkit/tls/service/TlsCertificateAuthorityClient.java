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
import org.apache.nifi.toolkit.tls.TlsToolkitMain;
import org.apache.nifi.toolkit.tls.configuration.TlsClientConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;

public class TlsCertificateAuthorityClient {
    private final File configFile;
    private final TlsHelper tlsHelper;
    private final PasswordUtil passwordUtil;
    private final TlsClientConfig tlsClientConfig;
    private final OutputStreamFactory outputStreamFactory;
    private final ObjectMapper objectMapper;
    private final TlsCertificateSigningRequestPerformer tlsCertificateSigningRequestPerformer;

    public TlsCertificateAuthorityClient(File configFile) throws IOException, NoSuchAlgorithmException {
        this(configFile, FileInputStream::new, FileOutputStream::new);
    }

    public TlsCertificateAuthorityClient(File configFile, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory)
            throws IOException, NoSuchAlgorithmException {
        this.configFile = configFile;
        this.objectMapper = new ObjectMapper();
        this.tlsClientConfig = objectMapper.readValue(inputStreamFactory.create(configFile), TlsClientConfig.class);
        this.tlsHelper = new TlsHelper(tlsClientConfig.getTlsHelperConfig());
        this.passwordUtil = new PasswordUtil(new SecureRandom());
        this.outputStreamFactory = outputStreamFactory;
        this.tlsCertificateSigningRequestPerformer = tlsClientConfig.createCertificateSigningRequestPerformer();
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        if (args.length != 1 || StringUtils.isEmpty(args[0])) {
            throw new Exception("Expected config file as only argument");
        }
        TlsCertificateAuthorityClient tlsCertificateAuthorityClient = new TlsCertificateAuthorityClient(new File(args[0]));
        if (tlsCertificateAuthorityClient.needsRun()) {
            tlsCertificateAuthorityClient.generateCertificateAndGetItSigned();
        }
    }

    public boolean needsRun() {
        return !(new File(tlsClientConfig.getKeyStore()).exists() && new File(tlsClientConfig.getTrustStore()).exists());
    }

    public void generateCertificateAndGetItSigned() throws Exception {
        KeyPair keyPair = tlsHelper.generateKeyPair();

        String keyStoreType = tlsClientConfig.getKeyStoreType();
        if (StringUtils.isEmpty(keyStoreType)) {
            keyStoreType = tlsHelper.getKeyStoreType();
            tlsClientConfig.setKeyStoreType(keyStoreType);
        }

        KeyStore keyStore = tlsHelper.createKeyStore(keyStoreType);
        String keyPassword = tlsClientConfig.getKeyPassword();
        if (StringUtils.isEmpty(keyPassword)) {
            keyPassword = passwordUtil.generatePassword();
            tlsClientConfig.setKeyPassword(keyPassword);
        }
        X509Certificate[] certificates = tlsCertificateSigningRequestPerformer.perform(objectMapper, keyPair);
        tlsHelper.addToKeyStore(keyStore, keyPair, TlsToolkitMain.NIFI_KEY, keyPassword.toCharArray(), certificates);

        String keyStorePassword = tlsClientConfig.getKeyStorePassword();
        if (StringUtils.isEmpty(keyStorePassword)) {
            keyStorePassword = passwordUtil.generatePassword();
            tlsClientConfig.setKeyStorePassword(keyStorePassword);
        }

        try (OutputStream outputStream = outputStreamFactory.create(new File(tlsClientConfig.getKeyStore()))) {
            keyStore.store(outputStream, keyStorePassword.toCharArray());
        }

        String trustStoreType = tlsClientConfig.getTrustStoreType();
        if (StringUtils.isEmpty(trustStoreType)) {
            trustStoreType = tlsHelper.getKeyStoreType();
            tlsClientConfig.setTrustStoreType(trustStoreType);
        }

        KeyStore trustStore = tlsHelper.createKeyStore(trustStoreType);
        trustStore.setCertificateEntry(TlsToolkitMain.NIFI_CERT, certificates[certificates.length - 1]);

        String trustStorePassword = tlsClientConfig.getTrustStorePassword();
        if (StringUtils.isEmpty(trustStorePassword)) {
            trustStorePassword = passwordUtil.generatePassword();
            tlsClientConfig.setTrustStorePassword(trustStorePassword);
        }

        try (OutputStream outputStream = outputStreamFactory.create(new File(tlsClientConfig.getTrustStore()))) {
            trustStore.store(outputStream, trustStorePassword.toCharArray());
        }

        try (OutputStream outputStream = outputStreamFactory.create(configFile)) {
            objectMapper.writeValue(outputStream, tlsClientConfig);
        }
    }
}
