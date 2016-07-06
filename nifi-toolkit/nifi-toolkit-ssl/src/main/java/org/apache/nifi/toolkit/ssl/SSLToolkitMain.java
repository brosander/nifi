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

import org.apache.nifi.util.NiFiProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SSLToolkitMain {
    private final SSLHelper sslHelper;
    private final File baseDir;
    private final List<String> nifiPropertyStrings;

    public SSLToolkitMain(SSLHelper sslHelper, File baseDir, List<String> nifiPropertyStrings) {
        this.sslHelper = sslHelper;
        this.baseDir = baseDir;
        this.nifiPropertyStrings = nifiPropertyStrings;
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SSLHelper sslHelper = new SSLHelper(new SecureRandom(), 365, 2048, "RSA", "SHA256WITHRSA", "jks");
        File baseDir = new File(".");
        List<String> nifiPropertyStrings = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(SSLToolkitMain.class.getClassLoader().getResourceAsStream("conf/nifi.properties")))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                nifiPropertyStrings.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            new SSLToolkitMain(sslHelper, baseDir, nifiPropertyStrings).createNifiKeystoresAndTrustStores("CN=nifi,OU=rootca", Arrays.asList("centos61", "centos62", "centos63"));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        }
    }

    public void createNifiKeystoresAndTrustStores(String dn, List<String> hostnames) throws GeneralSecurityException, IOException, OperatorCreationException {
        String extension = "." + sslHelper.getKeyStoreType().toLowerCase();

        KeyPair certificateKeypair = sslHelper.generateKeyPair();
        X509Certificate x509Certificate = sslHelper.generateSelfSignedX509Certificate(certificateKeypair, dn);

        String trustStoreName = "truststore" + extension;
        String trustStorePassword = sslHelper.generatePassword();

        for (String hostname : hostnames) {
            String keyPassword = sslHelper.generatePassword();
            String keyStorePassword = sslHelper.generatePassword();
            KeyPair keyPair = sslHelper.generateKeyPair();
            KeyStore keyStore = sslHelper.createKeyStore();
            sslHelper.addToKeyStore(keyStore, keyPair, "NIFI-KEY", keyPassword.toCharArray(),
                    sslHelper.generateIssuedCertificate("CN=nifi,OU=" + hostname, keyPair, x509Certificate, certificateKeypair), x509Certificate);

            String keyStoreName = hostname + extension;
            File propertiesFile = new File(baseDir, hostname + "-nifi.properties");
            try (BufferedWriter fileOutputStream = new BufferedWriter(new FileWriter(propertiesFile))) {
                for (String nifiPropertyString : nifiPropertyStrings) {
                    if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_KEYSTORE + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_KEYSTORE);
                        fileOutputStream.write("=./conf/");
                        fileOutputStream.write(keyStoreName);
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_KEYSTORE_TYPE + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_KEYSTORE_TYPE);
                        fileOutputStream.write("=");
                        fileOutputStream.write(sslHelper.getKeyStoreType());
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_KEYSTORE_PASSWD + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_KEYSTORE_PASSWD);
                        fileOutputStream.write("=");
                        fileOutputStream.write(keyStorePassword);
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_KEY_PASSWD + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_KEY_PASSWD);
                        fileOutputStream.write("=");
                        fileOutputStream.write(keyPassword);
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_TRUSTSTORE + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_TRUSTSTORE);
                        fileOutputStream.write("=./conf/truststore");
                        fileOutputStream.write(extension);
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_TRUSTSTORE_TYPE + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_TRUSTSTORE_TYPE);
                        fileOutputStream.write("=");
                        fileOutputStream.write(sslHelper.getKeyStoreType());
                    } else if (nifiPropertyString.startsWith(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD + "=")) {
                        fileOutputStream.write(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD);
                        fileOutputStream.write("=");
                        fileOutputStream.write(trustStorePassword);
                    } else {
                        fileOutputStream.write(nifiPropertyString);
                    }
                    fileOutputStream.newLine();
                }
            }

            File outputFile = new File(baseDir, keyStoreName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                keyStore.store(fileOutputStream, keyStorePassword.toCharArray());
            }
        }

        KeyStore trustStore = sslHelper.createKeyStore();
        trustStore.setCertificateEntry("NIFI-CERT", x509Certificate);

        File outputFile = new File(baseDir, trustStoreName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            trustStore.store(fileOutputStream, trustStorePassword.toCharArray());
        }
    }
}
