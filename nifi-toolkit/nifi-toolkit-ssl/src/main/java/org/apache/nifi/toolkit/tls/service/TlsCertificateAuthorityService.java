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
import org.apache.commons.io.input.BoundedReader;
import org.apache.nifi.toolkit.tls.TlsToolkitMain;
import org.apache.nifi.toolkit.tls.configuration.TlsConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.apache.nifi.toolkit.tls.util.PropertiesUtil;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class TlsCertificateAuthorityService extends AbstractHandler {
    private final KeyPair keyPair;
    private final X509Certificate caCert;
    private final TlsHelper tlsHelper;
    private final PasswordUtil passwordUtil;
    private final Server server;
    private final String token;

    public TlsCertificateAuthorityService(File configInput) throws Exception {
        this(configInput, FileInputStream::new, FileOutputStream::new);
    }

    public TlsCertificateAuthorityService(File configInput, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory) throws Exception {
        passwordUtil = new PasswordUtil(new SecureRandom());
        ObjectMapper objectMapper = new ObjectMapper();
        TlsConfig configuration = new TlsConfig(PropertiesUtil.loadToMap(inputStreamFactory.create(configInput)));
        tlsHelper = new TlsHelper(configuration.getTlsHelperConfig());
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
            KeyStore.Entry keyStoreEntry = keyStore.getEntry(TlsToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
            if (!KeyStore.PrivateKeyEntry.class.isInstance(keyStoreEntry)) {
                throw new IOException("Expected " + TlsToolkitMain.NIFI_KEY + " alias to contain a private key entry");
            }
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStoreEntry;
            keyPair = new KeyPair(privateKeyEntry.getCertificate().getPublicKey(), privateKeyEntry.getPrivateKey());
            Certificate[] certificateChain = privateKeyEntry.getCertificateChain();
            if (certificateChain.length != 1) {
                throw new IOException("Expected root ca cert to be only certificate in chain");
            }
            Certificate certificate = certificateChain[0];
            if (certificate instanceof X509Certificate) {
                caCert = (X509Certificate) certificate;
            } else {
                throw new IOException("Expected " + X509Certificate.class + " as root ca cert");
            }
        } else {
            keyPair = this.tlsHelper.generateKeyPair();
            caCert = this.tlsHelper.generateSelfSignedX509Certificate(keyPair, "CN=" + hostname + ",OU=NIFI");
            keyStore = this.tlsHelper.createKeyStore();
            String keyStorePassword = passwordUtil.generatePassword();
            keyPassword = passwordUtil.generatePassword();
            this.tlsHelper.addToKeyStore(keyStore, keyPair, TlsToolkitMain.NIFI_KEY, keyPassword.toCharArray(), caCert);
            try (OutputStream outputStream = outputStreamFactory.create(new File(keyStoreFile))) {
                keyStore.store(outputStream, keyStorePassword.toCharArray());
            }
            configuration.setKeyStoreType(this.tlsHelper.getKeyStoreType());
            configuration.setKeyStorePassword(keyStorePassword);
            configuration.setKeyPassword(keyPassword);
            Map<String, String> map = new HashMap<>();
            configuration.save(map);
            PropertiesUtil.saveFromMap(map, outputStreamFactory.create(configInput));
        }
        token = configuration.getToken();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyManagerPassword(keyPassword);

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

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        if (args.length != 1 || StringUtils.isEmpty(args[0])) {
            System.out.println("Expected configuration file as only argument");
        }
        TlsCertificateAuthorityService tlsCertificateAuthorityService = new TlsCertificateAuthorityService(new File(args[0]));
        tlsCertificateAuthorityService.start();
        System.out.println("Server Started");
        System.out.flush();
    }

    public void shutdown() throws Exception {
        server.stop();
        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TlsCertificateAuthorityRequest tlsCertificateAuthorityRequest = objectMapper.readValue(new BoundedReader(request.getReader(), 1024 * 1024), TlsCertificateAuthorityRequest.class);

            if (!tlsCertificateAuthorityRequest.hasCsr()) {
                writeResponse(objectMapper, response, new TlsCertificateAuthorityResponse("csr field must be set"), Response.SC_BAD_REQUEST);
                return;
            }

            if (!tlsCertificateAuthorityRequest.hasHmac()) {
                writeResponse(objectMapper, response, new TlsCertificateAuthorityResponse("hmac field must be set"), Response.SC_BAD_REQUEST);
                return;
            }

            JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = tlsCertificateAuthorityRequest.parseCsr();

            if (tlsHelper.checkHMac(tlsCertificateAuthorityRequest.getHmac(), token, jcaPKCS10CertificationRequest.getPublicKey())) {
                X509Certificate x509Certificate = tlsHelper.signCsr(jcaPKCS10CertificationRequest, this.caCert, keyPair);
                writeResponse(objectMapper, response, new TlsCertificateAuthorityResponse(tlsHelper.calculateHMac(token, caCert.getPublicKey()), x509Certificate), Response.SC_OK);
                return;
            } else {
                writeResponse(objectMapper, response, new TlsCertificateAuthorityResponse("forbidden"), Response.SC_FORBIDDEN);
                return;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void writeResponse(ObjectMapper objectMapper, HttpServletResponse response, TlsCertificateAuthorityResponse tlsCertificateAuthorityResponse, int responseCode) throws IOException {
        if (responseCode == Response.SC_OK) {
            objectMapper.writeValue(response.getWriter(), tlsCertificateAuthorityResponse);
            response.setStatus(responseCode);
        } else {
            response.sendError(responseCode, objectMapper.writeValueAsString(tlsCertificateAuthorityResponse));
        }
    }
}
