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

package org.apache.nifi.toolkit.ssl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.nifi.toolkit.ssl.SSLToolkitMain;
import org.apache.nifi.toolkit.ssl.configuration.SSLClientConfig;
import org.apache.nifi.toolkit.ssl.util.InputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.OutputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.PasswordUtil;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SSLCAClient {
    private final File configFile;
    private final SSLHelper sslHelper;
    private final PasswordUtil passwordUtil;
    private final SSLClientConfig sslClientConfig;
    private final OutputStreamFactory outputStreamFactory;
    private final ObjectMapper objectMapper;

    public SSLCAClient(File configFile) throws IOException, NoSuchAlgorithmException {
        this(configFile, FileInputStream::new, FileOutputStream::new);
    }

    public SSLCAClient(File configFile, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory)
            throws IOException, NoSuchAlgorithmException {
        this.configFile = configFile;
        this.objectMapper = new ObjectMapper();
        this.sslClientConfig = objectMapper.readValue(inputStreamFactory.create(configFile), SSLClientConfig.class);
        this.sslHelper = new SSLHelper(sslClientConfig.getSslHelper());
        this.passwordUtil = new PasswordUtil(new SecureRandom());
        this.outputStreamFactory = outputStreamFactory;
    }

    public void generateCertificateAndGetItSigned() throws Exception {
        List<X509Certificate> certificates = new ArrayList<>();
        KeyPair keyPair = sslHelper.generateKeyPair();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuilder.build()) {
            @Override
            public synchronized Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                                     InetSocketAddress localAddress, HttpContext context) throws IOException {
                Socket result = super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
                if (!SSLSocket.class.isInstance(result)) {
                    throw new IOException("Expected ssl socket");
                }
                SSLSocket sslSocket = (SSLSocket) result;
                javax.security.cert.X509Certificate[] peerCertificateChain = sslSocket.getSession().getPeerCertificateChain();
                if (peerCertificateChain.length != 1) {
                    throw new IOException("Expected root ca cert");
                }
                String cn;
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate(new X509CertificateHolder(Certificate.getInstance(peerCertificateChain[0].getEncoded())));
                    cn = IETFUtils.valueToString(new JcaX509CertificateHolder(certificate).getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue());
                    certificates.add(certificate);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                if (!sslClientConfig.getCaHostname().equals(cn)) {
                    throw new IOException("Expected cn of " + sslClientConfig.getCaHostname() + " but got " + cn);
                }
                return result;
            }
        });

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponseString;
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            HttpPost httpPost = new HttpPost();
            ObjectNode requestNode = objectMapper.createObjectNode();
            PKCS10CertificationRequest request = sslHelper.generateCertificationRequest("CN=" + sslClientConfig.getHostname() + ",OU=NIFI", keyPair);
            StringWriter requestStringWriter = new StringWriter();
            sslHelper.writeCertificationRequest(request, requestStringWriter);
            requestNode.put(SSLCAService.CSR, requestStringWriter.toString());
            requestNode.put(SSLCAService.HMAC, Base64.getEncoder().encodeToString(sslHelper.calculateHMac(sslClientConfig.getNonce(), keyPair.getPublic())));
            httpPost.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(requestNode)));
            try (CloseableHttpResponse response = client.execute(new HttpHost(sslClientConfig.getCaHostname(), 8443, "https"), httpPost)) {
                jsonResponseString = IOUtils.toString(new BoundedInputStream(response.getEntity().getContent(), 1024 * 1024), StandardCharsets.UTF_8);
            }
        }

        if (certificates.size() != 1) {
            throw new IOException("Expected one certificate");
        }

        JsonNode jsonResponseNode = objectMapper.readTree(jsonResponseString);
        JsonNode hmacNode = jsonResponseNode.get(SSLCAService.HMAC);
        String hmac;
        if (hmacNode == null || StringUtils.isEmpty(hmac = hmacNode.asText())) {
            throw new IOException("Expected response to contain hmac");
        }
        X509Certificate caCertificate = certificates.get(0);
        if (!sslHelper.checkHMac(hmac, sslClientConfig.getNonce(), caCertificate.getPublicKey())) {
            throw new IOException("Unexpected hmac received, possible man in the middle");
        }
        JsonNode certificateNode = jsonResponseNode.get(SSLCAService.CERTIFICATE);
        String certificateString;
        if (certificateNode == null || StringUtils.isEmpty(certificateString = certificateNode.asText())) {
            throw new IOException("Expected response to contain certificate");
        }
        X509Certificate x509Certificate = sslHelper.readCertificate(new StringReader(certificateString));
        KeyStore keyStore = sslHelper.createKeyStore();
        String keyPassword = passwordUtil.generatePassword();
        sslHelper.addToKeyStore(keyStore, keyPair, SSLToolkitMain.NIFI_KEY, keyPassword.toCharArray(), x509Certificate, caCertificate);
        String keyStorePassword = passwordUtil.generatePassword();
        try (OutputStream outputStream = outputStreamFactory.create(new File(sslClientConfig.getKeyStore()))) {
            keyStore.store(outputStream, keyStorePassword.toCharArray());
        }

        KeyStore trustStore = sslHelper.createKeyStore();
        trustStore.setCertificateEntry(SSLToolkitMain.NIFI_CERT, caCertificate);
        String trustStorePassword = passwordUtil.generatePassword();
        try (OutputStream outputStream = outputStreamFactory.create(new File(sslClientConfig.getTrustStore()))) {
            trustStore.store(outputStream, trustStorePassword.toCharArray());
        }

        sslClientConfig.setKeyStorePassword(keyStorePassword);
        sslClientConfig.setKeyPassword(keyPassword);
        sslClientConfig.setKeyStoreType(sslHelper.getKeyStoreType());

        sslClientConfig.setTrustStorePassword(trustStorePassword);
        sslClientConfig.setTrustStoreType(sslHelper.getKeyStoreType());

        try (OutputStream outputStream = outputStreamFactory.create(configFile)) {
            objectMapper.writer().writeValue(outputStream, sslClientConfig);
        }
    }
}
