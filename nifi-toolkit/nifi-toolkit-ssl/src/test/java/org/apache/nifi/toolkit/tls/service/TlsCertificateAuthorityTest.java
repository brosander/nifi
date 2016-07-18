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
import org.apache.nifi.toolkit.tls.configuration.TlsConfig;
import org.apache.nifi.toolkit.tls.configuration.TlsHelperConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PropertiesUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TlsCertificateAuthorityTest {
    private File serverConfigFile;
    private File clientConfigFile;
    private OutputStreamFactory outputStreamFactory;
    private InputStreamFactory inputStreamFactory;
    private TlsConfig serverConfig;
    private TlsClientConfig clientConfig;
    private ObjectMapper objectMapper;
    private TlsHelperConfig tlsHelperConfig;
    private ByteArrayOutputStream serverKeyStoreOutputStream;
    private ByteArrayOutputStream clientKeyStoreOutputStream;
    private ByteArrayOutputStream clientTrustStoreOutputStream;
    private ByteArrayOutputStream serverConfigFileOutputStream;
    private ByteArrayOutputStream clientConfigFileOutputStream;

    @BeforeClass
    public static void beforeClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() throws FileNotFoundException {
        objectMapper = new ObjectMapper();
        serverConfigFile = new File("fake.server.config");
        clientConfigFile = new File("fake.client.config");
        String serverKeyStore = "serverKeyStore";
        String clientKeyStore = "clientKeyStore";
        String clientTrustStore = "clientTrustStore";
        serverKeyStoreOutputStream = new ByteArrayOutputStream();
        clientKeyStoreOutputStream = new ByteArrayOutputStream();
        clientTrustStoreOutputStream = new ByteArrayOutputStream();
        serverConfigFileOutputStream = new ByteArrayOutputStream();
        clientConfigFileOutputStream = new ByteArrayOutputStream();

        String myTestTokenUseSomethingStronger = "myTestTokenUseSomethingStronger";
        int port = availablePort();

        serverConfig = new TlsConfig();
        serverConfig.setHostname("localhost");
        serverConfig.setToken(myTestTokenUseSomethingStronger);
        serverConfig.setKeyStore(serverKeyStore);
        serverConfig.setPort(port);

        clientConfig = new TlsClientConfig();
        clientConfig.setCaHostname("localhost");
        clientConfig.setHostname("otherHostname");
        clientConfig.setKeyStore(clientKeyStore);
        clientConfig.setTrustStore(clientTrustStore);
        clientConfig.setToken(myTestTokenUseSomethingStronger);
        clientConfig.setPort(port);

        tlsHelperConfig = new TlsHelperConfig();
        tlsHelperConfig.setDays(5);
        tlsHelperConfig.setKeySize(2048);
        serverConfig.setTlsHelperConfig(tlsHelperConfig);
        clientConfig.setTlsHelperConfig(tlsHelperConfig);

        outputStreamFactory = mock(OutputStreamFactory.class);
        mockReturnOutputStream(outputStreamFactory, new File(serverKeyStore), serverKeyStoreOutputStream);
        mockReturnOutputStream(outputStreamFactory, new File(clientKeyStore), clientKeyStoreOutputStream);
        mockReturnOutputStream(outputStreamFactory, new File(clientTrustStore), clientTrustStoreOutputStream);
        mockReturnOutputStream(outputStreamFactory, serverConfigFile, serverConfigFileOutputStream);
        mockReturnOutputStream(outputStreamFactory, clientConfigFile, clientConfigFileOutputStream);

        inputStreamFactory = mock(InputStreamFactory.class);
        mockReturnProperties(inputStreamFactory, serverConfigFile, serverConfig);
        mockReturnProperties(inputStreamFactory, clientConfigFile, clientConfig);
    }

    private void mockReturnProperties(InputStreamFactory inputStreamFactory, File file, TlsConfig tlsConfig) throws FileNotFoundException {
        when(inputStreamFactory.create(eq(file))).thenAnswer(invocation -> {
            Map<String, String> map = new HashMap<>();
            tlsConfig.save(map);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PropertiesUtil.saveFromMap(map, byteArrayOutputStream);
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        });
    }

    private void mockReturnOutputStream(OutputStreamFactory outputStreamFactory, File file, OutputStream outputStream) throws FileNotFoundException {
        when(outputStreamFactory.create(eq(file))).thenReturn(outputStream).thenReturn(null);
    }

    @Test
    public void testClientGetCert() throws Exception {
        TlsCertificateAuthorityService tlsCertificateAuthorityService = null;
        try {
            tlsCertificateAuthorityService = new TlsCertificateAuthorityService(serverConfigFile, inputStreamFactory, outputStreamFactory);
            TlsCertificateAuthorityClient tlsCertificateAuthorityClient = new TlsCertificateAuthorityClient(clientConfigFile, inputStreamFactory, outputStreamFactory);
            tlsCertificateAuthorityClient.generateCertificateAndGetItSigned();
            validate();
        } finally {
            if (tlsCertificateAuthorityService != null) {
                tlsCertificateAuthorityService.shutdown();
            }
        }
    }

    @Test
    public void testTokenMismatch() throws Exception {
        serverConfig.setToken("a different token...");
        try {
            testClientGetCert();
            fail("Expected error with mismatching token");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("forbidden"));
        }
    }

    private void validate() throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, SignatureException,
            NoSuchProviderException, UnrecoverableEntryException, IOException {
        Certificate caCertificate = validateServerKeyStore();
        validateClient(caCertificate);
    }

    private Certificate validateServerKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            InvalidKeyException, NoSuchProviderException, SignatureException {
        serverConfig = new TlsConfig(PropertiesUtil.loadToMap(new ByteArrayInputStream(serverConfigFileOutputStream.toByteArray())));

        KeyStore serverKeyStore = KeyStore.getInstance(serverConfig.getKeyStoreType());
        serverKeyStore.load(new ByteArrayInputStream(serverKeyStoreOutputStream.toByteArray()), serverConfig.getKeyStorePassword().toCharArray());
        KeyStore.Entry serverKeyEntry = serverKeyStore.getEntry(TlsToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(serverConfig.getKeyPassword().toCharArray()));

        assertTrue(serverKeyEntry instanceof KeyStore.PrivateKeyEntry);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) serverKeyEntry;
        Certificate[] certificateChain = privateKeyEntry.getCertificateChain();
        assertEquals(1, certificateChain.length);
        Certificate caCertificate = certificateChain[0];
        caCertificate.verify(caCertificate.getPublicKey());
        assertPrivateAndPublicKeyMatch(privateKeyEntry.getPrivateKey(), caCertificate.getPublicKey());
        return caCertificate;
    }

    private void validateClient(Certificate caCertificate) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableEntryException, InvalidKeyException, NoSuchProviderException, SignatureException {
        clientConfig = new TlsClientConfig(PropertiesUtil.loadToMap(new ByteArrayInputStream(clientConfigFileOutputStream.toByteArray())));

        KeyStore clientKeyStore = KeyStore.getInstance(clientConfig.getKeyStoreType());
        clientKeyStore.load(new ByteArrayInputStream(clientKeyStoreOutputStream.toByteArray()), clientConfig.getKeyStorePassword().toCharArray());
        KeyStore.Entry clientKeyStoreEntry = clientKeyStore.getEntry(TlsToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(clientConfig.getKeyPassword().toCharArray()));

        assertTrue(clientKeyStoreEntry instanceof KeyStore.PrivateKeyEntry);
        KeyStore.PrivateKeyEntry clientPrivateKeyEntry = (KeyStore.PrivateKeyEntry) clientKeyStoreEntry;
        Certificate[] certificateChain = clientPrivateKeyEntry.getCertificateChain();
        assertEquals(2, certificateChain.length);
        assertEquals(caCertificate, certificateChain[1]);
        certificateChain[0].verify(caCertificate.getPublicKey());
        assertPrivateAndPublicKeyMatch(clientPrivateKeyEntry.getPrivateKey(), certificateChain[0].getPublicKey());

        KeyStore clientTrustStore = KeyStore.getInstance(clientConfig.getTrustStoreType());
        clientTrustStore.load(new ByteArrayInputStream(clientTrustStoreOutputStream.toByteArray()), clientConfig.getTrustStorePassword().toCharArray());
        assertEquals(caCertificate, clientTrustStore.getCertificate(TlsToolkitMain.NIFI_CERT));
    }

    private void assertPrivateAndPublicKeyMatch(PrivateKey privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(TlsHelperConfig.DEFAULT_SIGNING_ALGORITHM);
        signature.initSign(privateKey);
        byte[] bytes = "test string".getBytes(StandardCharsets.UTF_8);
        signature.update(bytes);

        Signature verify = Signature.getInstance(TlsHelperConfig.DEFAULT_SIGNING_ALGORITHM);
        verify.initVerify(publicKey);
        verify.update(bytes);
        verify.verify(signature.sign());
    }

    /**
     * Will determine the available port used by ca server
     */
    private int availablePort() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(0);
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to discover available port.", e);
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
