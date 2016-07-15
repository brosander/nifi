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
import org.apache.commons.io.input.BoundedReader;
import org.apache.nifi.toolkit.ssl.SSLToolkitMain;
import org.apache.nifi.toolkit.ssl.configuration.SSLConfig;
import org.apache.nifi.toolkit.ssl.util.InputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.OutputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.PasswordUtil;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
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
import java.io.StringReader;
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
    private final String hostname;

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
        hostname = configuration.getHostname();
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
        sslConnector.setPort(8443);

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
            JsonNode jsonNode = objectMapper.readTree(new BoundedReader(request.getReader(), 1024 * 1024));
            ObjectNode responseNode = objectMapper.createObjectNode();
            JsonNode csrNode = jsonNode.get(CSR);
            String csrText;
            if (csrNode == null || StringUtils.isEmpty(csrText = csrNode.asText())) {
                responseNode.put(ERROR, "csr field must be set");
                writeResponse(objectMapper, response, responseNode, Response.SC_BAD_REQUEST);
                return;
            }

            JsonNode hmacNode = jsonNode.get(HMAC);
            String hmac;
            if (hmacNode == null || StringUtils.isEmpty(hmac = hmacNode.asText())) {
                responseNode.put(ERROR, "hmac field must be set");
                writeResponse(objectMapper, response, responseNode, Response.SC_BAD_REQUEST);
                return;
            }

            PKCS10CertificationRequest csr = sslHelper.readCertificationRequest(new StringReader(csrText));
            JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = new JcaPKCS10CertificationRequest(csr);

            if (sslHelper.checkHMac(hmac, nonce, sslHelper.getKeyIdentifier(jcaPKCS10CertificationRequest.getPublicKey()))) {
                StringWriter signedCertificate = new StringWriter();
                sslHelper.writeCertificate(sslHelper.signCsr(jcaPKCS10CertificationRequest, this.caCert, keyPair), signedCertificate);

                responseNode.put(HMAC, Base64.getEncoder().encodeToString(sslHelper.calculateHMac(nonce, caCert.getPublicKey())));
                responseNode.put(CERTIFICATE, signedCertificate.toString());
                writeResponse(objectMapper, response, responseNode, Response.SC_OK);
                return;
            } else {
                responseNode.put(ERROR, "forbidden");
                writeResponse(objectMapper, response, responseNode, Response.SC_FORBIDDEN);
                return;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void writeResponse(ObjectMapper objectMapper, HttpServletResponse response, ObjectNode responseNode, int responseCode) throws IOException {
        objectMapper.writeTree(objectMapper.getFactory().createGenerator(response.getWriter()), responseNode);
        response.setStatus(responseCode);
    }
}
