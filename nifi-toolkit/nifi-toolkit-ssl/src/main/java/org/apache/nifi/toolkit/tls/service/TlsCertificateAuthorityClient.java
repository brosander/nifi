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
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.nifi.toolkit.tls.TlsToolkitMain;
import org.apache.nifi.toolkit.tls.configuration.TlsClientConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.apache.nifi.toolkit.tls.util.PropertiesUtil;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.eclipse.jetty.server.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TlsCertificateAuthorityClient {
    private final File configFile;
    private final TlsHelper tlsHelper;
    private final PasswordUtil passwordUtil;
    private final TlsClientConfig tlsClientConfig;
    private final OutputStreamFactory outputStreamFactory;
    private final ObjectMapper objectMapper;

    public TlsCertificateAuthorityClient(File configFile) throws IOException, NoSuchAlgorithmException {
        this(configFile, FileInputStream::new, FileOutputStream::new);
    }

    public TlsCertificateAuthorityClient(File configFile, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory)
            throws IOException, NoSuchAlgorithmException {
        this.configFile = configFile;
        this.objectMapper = new ObjectMapper();
        this.tlsClientConfig = new TlsClientConfig(PropertiesUtil.loadToMap(inputStreamFactory.create(configFile)));
        this.tlsHelper = new TlsHelper(tlsClientConfig.getTlsHelperConfig());
        this.passwordUtil = new PasswordUtil(new SecureRandom());
        this.outputStreamFactory = outputStreamFactory;
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
        List<X509Certificate> certificates = new ArrayList<>();
        KeyPair keyPair = tlsHelper.generateKeyPair();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();

        // We will be validating that we are talking to the correct host once we get the response's hmac of the token and public key of the ca
        sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        httpClientBuilder.setSSLSocketFactory(new TlsCertificateAuthorityClientSocketFactory(sslContextBuilder.build(), tlsClientConfig.getCaHostname(), certificates));

        String jsonResponseString;
        int responseCode;
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            JcaPKCS10CertificationRequest request = tlsHelper.generateCertificationRequest("CN=" + tlsClientConfig.getHostname() + ",OU=NIFI", keyPair);
            TlsCertificateAuthorityRequest tlsCertificateAuthorityRequest = new TlsCertificateAuthorityRequest(tlsHelper.calculateHMac(tlsClientConfig.getToken(), request.getPublicKey()), request);

            HttpPost httpPost = new HttpPost();
            httpPost.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(tlsCertificateAuthorityRequest)));

            try (CloseableHttpResponse response = client.execute(new HttpHost(tlsClientConfig.getCaHostname(), tlsClientConfig.getPort(), "https"), httpPost)) {
                jsonResponseString = IOUtils.toString(new BoundedInputStream(response.getEntity().getContent(), 1024 * 1024), StandardCharsets.UTF_8);
                responseCode = response.getStatusLine().getStatusCode();
            }
        }

        if (responseCode != Response.SC_OK) {
            throw new IOException("Received response code " + responseCode + " with payload " + jsonResponseString);
        }

        if (certificates.size() != 1) {
            throw new IOException("Expected one certificate");
        }

        TlsCertificateAuthorityResponse tlsCertificateAuthorityResponse = objectMapper.readValue(jsonResponseString, TlsCertificateAuthorityResponse.class);
        if (!tlsCertificateAuthorityResponse.hasHmac()) {
            throw new IOException("Expected response to contain hmac");
        }

        X509Certificate caCertificate = certificates.get(0);
        if (!tlsHelper.checkHMac(tlsCertificateAuthorityResponse.getHmac(), tlsClientConfig.getToken(), caCertificate.getPublicKey())) {
            throw new IOException("Unexpected hmac received, possible man in the middle");
        }

        if (!tlsCertificateAuthorityResponse.hasCertificate()) {
            throw new IOException("Expected response to contain certificate");
        }

        X509Certificate x509Certificate = tlsCertificateAuthorityResponse.parseCertificate();
        x509Certificate.verify(caCertificate.getPublicKey());

        KeyStore keyStore = tlsHelper.createKeyStore();
        String keyPassword = passwordUtil.generatePassword();
        tlsHelper.addToKeyStore(keyStore, keyPair, TlsToolkitMain.NIFI_KEY, keyPassword.toCharArray(), x509Certificate, caCertificate);
        String keyStorePassword = passwordUtil.generatePassword();
        try (OutputStream outputStream = outputStreamFactory.create(new File(tlsClientConfig.getKeyStore()))) {
            keyStore.store(outputStream, keyStorePassword.toCharArray());
        }

        KeyStore trustStore = tlsHelper.createKeyStore();
        trustStore.setCertificateEntry(TlsToolkitMain.NIFI_CERT, caCertificate);
        String trustStorePassword = passwordUtil.generatePassword();
        try (OutputStream outputStream = outputStreamFactory.create(new File(tlsClientConfig.getTrustStore()))) {
            trustStore.store(outputStream, trustStorePassword.toCharArray());
        }

        tlsClientConfig.setKeyStorePassword(keyStorePassword);
        tlsClientConfig.setKeyPassword(keyPassword);
        tlsClientConfig.setKeyStoreType(tlsHelper.getKeyStoreType());

        tlsClientConfig.setTrustStorePassword(trustStorePassword);
        tlsClientConfig.setTrustStoreType(tlsHelper.getKeyStoreType());

        Map<String, String> map = new HashMap<>();
        tlsClientConfig.save(map);
        try (OutputStream outputStream = outputStreamFactory.create(configFile)) {
            PropertiesUtil.saveFromMap(map, outputStream);
        }
    }
}
