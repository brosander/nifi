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

import org.apache.nifi.toolkit.ssl.commandLine.SSLToolkitCommandLine;
import org.apache.nifi.toolkit.ssl.commandLine.CommandLineParseException;
import org.apache.nifi.toolkit.ssl.configuration.SSLHostConfigurationBuilder;
import org.apache.nifi.toolkit.ssl.properties.NiFiPropertiesWriterFactory;
import org.apache.nifi.toolkit.ssl.util.TlsHelper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.apache.nifi.toolkit.ssl.commandLine.SSLToolkitCommandLine.ERROR_GENERATING_CONFIG;

public class TlsToolkitMain {
    public static final String NIFI_KEY = "nifi-key";
    public static final String NIFI_CERT = "nifi-cert";
    public static final String ROOT_CERT_PRIVATE_KEY = "rootCert.key";
    public static final String ROOT_CERT_CRT = "rootCert.crt";
    public static final String NIFI_PROPERTIES = "nifi.properties";

    private final TlsHelper tlsHelper;
    private final File baseDir;
    private final NiFiPropertiesWriterFactory niFiPropertiesWriterFactory;

    public TlsToolkitMain(TlsHelper tlsHelper, File baseDir, NiFiPropertiesWriterFactory niFiPropertiesWriterFactory) {
        this.tlsHelper = tlsHelper;
        this.baseDir = baseDir;
        this.niFiPropertiesWriterFactory = niFiPropertiesWriterFactory;
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SSLToolkitCommandLine sslToolkitCommandLine = new SSLToolkitCommandLine(new SecureRandom());
        try {
            sslToolkitCommandLine.parse(args);
        } catch (CommandLineParseException e) {
            System.exit(e.getExitCode());
        }
        try {
            new TlsToolkitMain(new TlsHelper(sslToolkitCommandLine), sslToolkitCommandLine.getBaseDir(), sslToolkitCommandLine.getNiFiPropertiesWriterFactory())
                    .createNifiKeystoresAndTrustStores("CN=nifi.root.ca,OU=apache.nifi", sslToolkitCommandLine.getHostnames(), sslToolkitCommandLine.getKeyStorePasswords(),
                            sslToolkitCommandLine.getKeyPasswords(), sslToolkitCommandLine.getTrustStorePasswords(), sslToolkitCommandLine.getHttpsPort());
        } catch (Exception e) {
            sslToolkitCommandLine.printUsage("Error creating generating ssl configuration. (" + e.getMessage() + ")");
            System.exit(ERROR_GENERATING_CONFIG);
        }
        System.exit(0);
    }

    public void createNifiKeystoresAndTrustStores(String dn, List<String> hostnames, List<String> keyStorePasswords, List<String> keyPasswords,
                                                  List<String> trustStorePasswords, String httpsPort) throws GeneralSecurityException, IOException, OperatorCreationException {
        KeyPair certificateKeypair = tlsHelper.generateKeyPair();
        X509Certificate x509Certificate = tlsHelper.generateSelfSignedX509Certificate(certificateKeypair, dn);

        try (PemWriter pemWriter = new PemWriter(new FileWriter(new File(baseDir, ROOT_CERT_CRT)))) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(x509Certificate));
        }

        try (PemWriter pemWriter = new PemWriter(new FileWriter(new File(baseDir, ROOT_CERT_PRIVATE_KEY)))) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(certificateKeypair));
        }

        KeyStore trustStore = tlsHelper.createKeyStore();
        trustStore.setCertificateEntry(NIFI_CERT, x509Certificate);

        SSLHostConfigurationBuilder sslHostConfigurationBuilder = new SSLHostConfigurationBuilder(tlsHelper, niFiPropertiesWriterFactory)
                .setHttpsPort(httpsPort)
                .setCertificateKeypair(certificateKeypair)
                .setX509Certificate(x509Certificate)
                .setTrustStore(trustStore);

        for (int i = 0; i < hostnames.size(); i++) {
            String hostname = hostnames.get(i);
            File hostDir = new File(baseDir, hostname);

            if (!hostDir.mkdirs()) {
                throw new IOException("Unable to make directory: " + hostDir.getAbsolutePath());
            }

            sslHostConfigurationBuilder
                    .setHostDir(hostDir)
                    .setKeyStorePassword(keyStorePasswords.get(i))
                    .setKeyPassword(keyPasswords.get(i))
                    .setTrustStorePassword(trustStorePasswords.get(i))
                    .setHostname(hostname)
                    .createSSLHostConfiguration()
                    .processHost();
        }
    }
}
