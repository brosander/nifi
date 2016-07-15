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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.BoundedReader;
import org.apache.nifi.toolkit.ssl.SSLToolkitMain;
import org.apache.nifi.toolkit.ssl.configuration.SSLConfig;
import org.apache.nifi.toolkit.ssl.util.InputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.OutputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.PasswordUtil;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SSLCAService extends AbstractHandler {
    public static final String CSR = "csr";
    public static final String HMAC = "hmac";
    public static final String CERTIFICATE = "certificate";
    public static final String ERROR = "error";
    private final KeyPair keyPair;
    private final X509Certificate caCert;
    private final SSLHelper sslHelper;
    private final PasswordUtil passwordUtil;
    private final Server server;
    private final String nonce;

    public SSLCAService(File configInput) throws Exception {
        this(configInput, FileInputStream::new, FileOutputStream::new);
    }

    public SSLCAService(File configInput, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory) throws Exception {
        passwordUtil = new PasswordUtil(new SecureRandom());
        ObjectMapper objectMapper = new ObjectMapper();
        SSLConfig configuration = objectMapper.readValue(inputStreamFactory.create(configInput), SSLConfig.class);
        sslHelper = new SSLHelper(configuration.getSslHelper());
        String keyStoreFile = configuration.getKeyStore();
        KeyStore keyStore;
        String keyPassword;
        String hostname = configuration.getHostname();
        if (new File(keyStoreFile).exists()) {
            keyStore = KeyStore.getInstance(configuration.getKeyStoreType());
            try (InputStream inputStream = new FileInputStream(keyStoreFile)) {
                keyStore.load(inputStream, configuration.getKeyStorePassword().toCharArray());
            }
            keyPassword = configuration.getKeyPassword();
            KeyStore.Entry keyStoreEntry = keyStore.getEntry(SSLToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
            if (!KeyStore.PrivateKeyEntry.class.isInstance(keyStoreEntry)) {
                throw new IOException("Expected " + SSLToolkitMain.NIFI_KEY + " alias to contain a private key entry");
            }
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStoreEntry;
            keyPair = new KeyPair(privateKeyEntry.getCertificate().getPublicKey(), privateKeyEntry.getPrivateKey());
            caCert = this.sslHelper.readCertificate(new InputStreamReader(new ByteArrayInputStream(privateKeyEntry.getCertificate().getEncoded())));
        } else {
            keyPair = this.sslHelper.generateKeyPair();
            caCert = this.sslHelper.generateSelfSignedX509Certificate(keyPair, "CN=" + hostname + ",OU=NIFI");
            keyStore = this.sslHelper.createKeyStore();
            String keyStorePassword = passwordUtil.generatePassword();
            keyPassword = passwordUtil.generatePassword();
            this.sslHelper.addToKeyStore(keyStore, keyPair, SSLToolkitMain.NIFI_KEY, keyPassword.toCharArray(), caCert);
            try (OutputStream outputStream = outputStreamFactory.create(new File(keyStoreFile))) {
                keyStore.store(outputStream, keyStorePassword.toCharArray());
            }
            configuration.setKeyStoreType(this.sslHelper.getKeyStoreType());
            configuration.setKeyStorePassword(keyStorePassword);
            configuration.setKeyPassword(keyPassword);
            objectMapper.writeValue(outputStreamFactory.create(configInput), configuration);
        }
        nonce = configuration.getNonce();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyManagerPassword(keyPassword);
        sslContextFactory.setIncludeCipherSuites(configuration.getSslCipher());

        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        server = new Server();

        ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(configuration.getPort());

        server.addConnector(sslConnector);
        server.setHandler(this);

        server.start();
        server.dumpStdErr();
    }

    public void shutdown() throws Exception {
        server.stop();
        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SSLCARequest sslcaRequest = objectMapper.readValue(new BoundedReader(request.getReader(), 1024 * 1024), SSLCARequest.class);
            SSLCAResponse sslcaResponse = new SSLCAResponse();

            if (StringUtils.isEmpty(sslcaRequest.getCsr())) {
                sslcaResponse.setError("csr field must be set");
                writeResponse(objectMapper, response, sslcaResponse, Response.SC_BAD_REQUEST);
                return;
            }

            if (StringUtils.isEmpty(sslcaRequest.getHmac())) {
                sslcaResponse.setError("hmac field must be set");
                writeResponse(objectMapper, response, sslcaResponse, Response.SC_BAD_REQUEST);
                return;
            }

            JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = sslcaRequest.parseCsr();

            if (sslHelper.checkHMac(sslcaRequest.getHmac(), nonce, sslHelper.getKeyIdentifier(jcaPKCS10CertificationRequest.getPublicKey()))) {
                StringWriter signedCertificate = new StringWriter();
                sslHelper.writeCertificate(sslHelper.signCsr(jcaPKCS10CertificationRequest, this.caCert, keyPair), signedCertificate);

                sslcaResponse.setHmac(Base64.getEncoder().encodeToString(sslHelper.calculateHMac(nonce, caCert.getPublicKey())));
                sslcaResponse.setCertificate(signedCertificate.toString());
                writeResponse(objectMapper, response, sslcaResponse, Response.SC_OK);
                return;
            } else {
                sslcaResponse.setError("forbidden");
                writeResponse(objectMapper, response, sslcaResponse, Response.SC_FORBIDDEN);
                return;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void writeResponse(ObjectMapper objectMapper, HttpServletResponse response, SSLCAResponse sslcaResponse, int responseCode) throws IOException {
        if (responseCode == Response.SC_OK) {
            objectMapper.writeValue(response.getWriter(), sslcaResponse);
            response.setStatus(responseCode);
        } else {
            response.sendError(responseCode, objectMapper.writeValueAsString(sslcaResponse));
        }
    }
}
