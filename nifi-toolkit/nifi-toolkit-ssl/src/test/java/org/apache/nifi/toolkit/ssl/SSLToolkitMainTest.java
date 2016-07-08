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

package org.apache.nifi.toolkit.ssl;

import org.apache.commons.io.FileUtils;
import org.apache.nifi.util.NiFiProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Permission;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SSLToolkitMainTest {
    private SecurityManager originalSecurityManager;

    @Before
    public void setup() {
        originalSecurityManager = System.getSecurityManager();
        // [see http://stackoverflow.com/questions/309396/java-how-to-test-methods-that-call-system-exit#answer-309427]
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                // Noop
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Noop
            }

            @Override
            public void checkExit(int status) {
                super.checkExit(status);
                throw new ExitException(status);
            }
        });
    }

    @After
    public void teardown() {
        System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testBadParse() {
        runAndAssertExitCode(SSLToolkitMain.ERROR_PARSING_COMMAND_LINE, "--unknownArgument");
    }

    @Test
    public void testHelp() {
        runAndAssertExitCode(SSLToolkitMain.HELP_EXIT_CODE, "-h");
        runAndAssertExitCode(SSLToolkitMain.HELP_EXIT_CODE, "--help");
    }

    @Test
    public void testDirOutput() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException, InvalidKeyException, NoSuchProviderException, SignatureException, InvalidKeySpecException {
        File tempDir = File.createTempFile("ssl-test", UUID.randomUUID().toString());
        if (!tempDir.delete()) {
            throw new IOException("Couldn't delete " + tempDir);
        }

        if (!tempDir.mkdirs()) {
            throw new IOException("Couldn't make directory " + tempDir);
        }

        try {
            runAndAssertExitCode(0, "-o", tempDir.getAbsolutePath());

            File hostDir = new File(tempDir, SSLToolkitMain.DEFAULT_HOSTNAMES);

            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(FileUtils.readFileToByteArray(new File(hostDir, SSLToolkitMain.ROOT_CERT_PRIVATE_KEY)));

            KeyFactory.getInstance(SSLToolkitMain.DEFAULT_KEY_ALGORITHM).generatePrivate(pkcs8EncodedKeySpec);

            Properties nifiProperties = new Properties();
            try (InputStream inputStream = new FileInputStream(new File(hostDir, "nifi.properties"))) {
                nifiProperties.load(inputStream);
            }

            String trustStoreType = nifiProperties.getProperty(NiFiProperties.SECURITY_TRUSTSTORE_TYPE);
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            try (InputStream inputStream = new FileInputStream(new File(hostDir, "truststore." + trustStoreType))) {
                trustStore.load(inputStream, nifiProperties.getProperty(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD).toCharArray());
            }

            Certificate certificate = trustStore.getCertificate(SSLToolkitMain.NIFI_CERT);

            String keyStoreType = nifiProperties.getProperty(NiFiProperties.SECURITY_KEYSTORE_TYPE);
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            try (InputStream inputStream = new FileInputStream(new File(hostDir, SSLToolkitMain.DEFAULT_HOSTNAMES + "." + keyStoreType))) {
                keyStore.load(inputStream, nifiProperties.getProperty(NiFiProperties.SECURITY_KEYSTORE_PASSWD).toCharArray());
            }

            char[] keyPassword = nifiProperties.getProperty(NiFiProperties.SECURITY_KEY_PASSWD).toCharArray();

            KeyStore.Entry entry = keyStore.getEntry(SSLToolkitMain.NIFI_KEY, new KeyStore.PasswordProtection(keyPassword));
            assertEquals(KeyStore.PrivateKeyEntry.class, entry.getClass());

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

            Certificate[] certificateChain = privateKeyEntry.getCertificateChain();

            assertEquals(2, certificateChain.length);
            assertEquals(certificate, certificateChain[1]);
            certificateChain[1].verify(certificate.getPublicKey());
            certificateChain[0].verify(certificate.getPublicKey());
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private void runAndAssertExitCode(int exitCode, String... args) {
        try {
            SSLToolkitMain.main(args);
            fail("Expecting exit code: " + exitCode);
        } catch (ExitException e) {
            assertEquals(exitCode, e.getExitCode());
        }
    }

    private static class ExitException extends SecurityException {
        private final int exitCode;

        public ExitException(int exitCode) {
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
