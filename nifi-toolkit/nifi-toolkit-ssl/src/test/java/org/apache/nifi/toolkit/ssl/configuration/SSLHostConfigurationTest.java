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

package org.apache.nifi.toolkit.ssl.configuration;

import org.apache.nifi.toolkit.ssl.TlsToolkitMain;
import org.apache.nifi.toolkit.ssl.commandLine.SSLToolkitCommandLine;
import org.apache.nifi.toolkit.ssl.properties.NiFiPropertiesWriter;
import org.apache.nifi.toolkit.ssl.properties.NiFiPropertiesWriterFactory;
import org.apache.nifi.toolkit.ssl.util.OutputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.toolkit.ssl.util.SSLHelperTest;
import org.apache.nifi.util.NiFiProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SSLHostConfigurationTest {
    SSLHostConfiguration sslHostConfiguration;

    private SSLHelper sslHelper;
    private NiFiPropertiesWriterFactory niFiPropertiesWriterFactory;
    private String httpsPort;
    private KeyPair certificateKeypair;
    private X509Certificate x509Certificate;
    private KeyStore trustStore;
    private File hostDir;
    private String keyStorePassword;
    private String keyPassword;
    private String trustStorePassword;
    private String hostname;
    private OutputStreamFactory outputStreamFactory;
    private File nifiPropertiesFile;
    private File keystoreFile;
    private File truststoreFile;
    private ByteArrayOutputStream nifiPropertiesOutputStream;
    private ByteArrayOutputStream keystoreOutputStream;
    private ByteArrayOutputStream truststoreOutputStream;
    private NiFiPropertiesWriter niFiPropertiesWriter;
    private KeyStore keyStore;
    private Properties nifiProperties;

    @BeforeClass
    public static void beforeClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() throws Exception {
        httpsPort = "8443";
        certificateKeypair = SSLHelperTest.loadKeyPair(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("rootCert.key")));
        x509Certificate = SSLHelperTest.loadCertificate(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("rootCert.crt")));
        nifiProperties = new Properties();
        nifiProperties.load(getClass().getClassLoader().getResourceAsStream("localhost/nifi.properties"));
        trustStore = KeyStore.getInstance("jks");
        trustStore.load(getClass().getClassLoader().getResourceAsStream("localhost/truststore.jks"), nifiProperties.getProperty(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD).toCharArray());
        hostDir = new File("/test/path/doesnt/exist");
        keyStorePassword = "testKeyStorePassword";
        keyPassword = "testKeyPassword";
        trustStorePassword = "testTrustStorePassword";
        hostname = "testHostName";
        buildSslHostConfiguration();
    }

    private void buildSslHostConfiguration() throws Exception {
        sslHelper = new SSLHelper(Integer.parseInt(SSLToolkitCommandLine.DEFAULT_CERT_DAYS), Integer.parseInt(SSLToolkitCommandLine.DEFAULT_KEYSIZE),
                SSLToolkitCommandLine.DEFAULT_KEY_ALGORITHM, SSLToolkitCommandLine.DEFAULT_SIGNING_ALGORITHM, SSLToolkitCommandLine.DEFAULT_KEY_STORE_TYPE);
        keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, null);

        niFiPropertiesWriterFactory = mock(NiFiPropertiesWriterFactory.class);
        outputStreamFactory = mock(OutputStreamFactory.class);
        niFiPropertiesWriter = mock(NiFiPropertiesWriter.class);

        nifiPropertiesFile = new File(hostDir, SSLHostConfiguration.NIFI_PROPERTIES);
        keystoreFile = new File(hostDir, hostname + "." + sslHelper.getKeyStoreType());
        truststoreFile = new File(hostDir, SSLHostConfiguration.TRUSTSTORE + "." + sslHelper.getKeyStoreType());

        nifiPropertiesOutputStream = new ByteArrayOutputStream();
        keystoreOutputStream = new ByteArrayOutputStream();
        truststoreOutputStream = new ByteArrayOutputStream();

        when(niFiPropertiesWriterFactory.create()).thenReturn(niFiPropertiesWriter);
        when(outputStreamFactory.create(eq(nifiPropertiesFile))).thenReturn(nifiPropertiesOutputStream);
        when(outputStreamFactory.create(eq(keystoreFile))).thenReturn(keystoreOutputStream);
        when(outputStreamFactory.create(eq(truststoreFile))).thenReturn(truststoreOutputStream);

        sslHostConfiguration = new SSLHostConfigurationBuilder(sslHelper, niFiPropertiesWriterFactory)
                .setOutputStreamFactory(outputStreamFactory)
                .setHttpsPort(httpsPort)
                .setCertificateKeypair(certificateKeypair)
                .setX509Certificate(x509Certificate)
                .setTrustStore(trustStore)
                .setHostDir(hostDir)
                .setKeyStorePassword(keyStorePassword)
                .setKeyPassword(keyPassword)
                .setTrustStorePassword(trustStorePassword)
                .setHostname(hostname)
                .createSSLHostConfiguration();
    }

    @Test
    public void testHttpsPort() throws GeneralSecurityException, IOException, OperatorCreationException {
        sslHostConfiguration.processHost();
        verify(niFiPropertiesWriter).setPropertyValue(NiFiProperties.WEB_HTTPS_PORT, httpsPort);
        verify(niFiPropertiesWriter).setPropertyValue(eq(NiFiProperties.WEB_HTTP_PORT), eq(""));
        verify(niFiPropertiesWriter).setPropertyValue(NiFiProperties.SITE_TO_SITE_SECURE, "true");
    }

    @Test
    public void setNullHttpsPort() throws Exception {
        httpsPort = null;
        buildSslHostConfiguration();
        sslHostConfiguration.processHost();
        verify(niFiPropertiesWriter, never()).setPropertyValue(eq(NiFiProperties.WEB_HTTPS_PORT), anyString());
        verify(niFiPropertiesWriter, never()).setPropertyValue(eq(NiFiProperties.WEB_HTTP_PORT), anyString());
        verify(niFiPropertiesWriter, never()).setPropertyValue(eq(NiFiProperties.SITE_TO_SITE_SECURE), anyString());
    }

    @Test
    public void testExtension() throws GeneralSecurityException, IOException, OperatorCreationException {
        sslHostConfiguration.processHost();
        verify(outputStreamFactory).create(eq(keystoreFile));
        verify(outputStreamFactory).create(eq(truststoreFile));
    }

    @Test
    public void testCertificates() throws GeneralSecurityException, IOException, OperatorCreationException {
        sslHostConfiguration.processHost();

        KeyStore trustStore = KeyStore.getInstance("jks");
        trustStore.load(new ByteArrayInputStream(truststoreOutputStream.toByteArray()), trustStorePassword.toCharArray());
        Certificate certificate = trustStore.getCertificate(TlsToolkitMain.NIFI_CERT);
        assertEquals(x509Certificate, certificate);

        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new ByteArrayInputStream(keystoreOutputStream.toByteArray()), keyStorePassword.toCharArray());
        KeyStore.Entry entry = keyStore.getEntry(TlsToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
        assertEquals(KeyStore.PrivateKeyEntry.class, entry.getClass());
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

        Certificate[] certificateChain = privateKeyEntry.getCertificateChain();
        assertEquals(2, certificateChain.length);
        certificateChain[0].verify(certificate.getPublicKey());
        certificateChain[1].verify(certificate.getPublicKey());
        assertEquals(certificate, certificateChain[1]);
    }
}
