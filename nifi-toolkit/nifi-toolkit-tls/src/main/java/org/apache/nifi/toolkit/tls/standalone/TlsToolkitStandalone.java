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

package org.apache.nifi.toolkit.tls.standalone;

import org.apache.nifi.security.util.CertificateUtils;
import org.apache.nifi.toolkit.tls.ca.CertificateAuthority;
import org.apache.nifi.toolkit.tls.commandLine.impl.StandaloneCommandLine;
import org.apache.nifi.toolkit.tls.configuration.CertificateAuthorityClientConfig;
import org.apache.nifi.toolkit.tls.configuration.InstanceDefinition;
import org.apache.nifi.toolkit.tls.configuration.StandaloneConfig;
import org.apache.nifi.toolkit.tls.configuration.writer.NiFiPropertiesWriterFactory;
import org.apache.nifi.toolkit.tls.configuration.writer.NifiPropertiesConfigurationWriter;
import org.apache.nifi.toolkit.tls.keystore.CertificateAuthorityKeyStoreManager;
import org.apache.nifi.toolkit.tls.service.TlsToolkitClient;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static sun.security.krb5.internal.ktab.KeyTabConstants.keySize;

public class TlsToolkitStandalone {
    public static final String NIFI_KEY = "nifi-key";
    public static final String NIFI_CERT = "nifi-cert";
    public static final String NIFI_PROPERTIES = "nifi.properties";

    private final Logger logger = LoggerFactory.getLogger(TlsToolkitStandalone.class);
    private final OutputStreamFactory outputStreamFactory;

    public TlsToolkitStandalone() {
        this(FileOutputStream::new);
    }

    public TlsToolkitStandalone(OutputStreamFactory outputStreamFactory) {
        this.outputStreamFactory = outputStreamFactory;
    }

    protected static String getClientDnFile(String clientDn) {
        return clientDn.replace(',', '_').replace(' ', '_');
    }

    public void createNifiKeystoresAndTrustStores(StandaloneConfig standaloneConfig) throws GeneralSecurityException, IOException {
        File baseDir = standaloneConfig.getBaseDir();
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException(baseDir + " doesn't exist and unable to create it.");
        }

        if (!baseDir.isDirectory()) {
            throw new IOException("Expected directory to output to");
        }

        CertificateAuthorityKeyStoreManager certificateAuthorityKeyStoreManager = new CertificateAuthorityKeyStoreManager(standaloneConfig.getCertificateAuthorityConfig(), baseDir);
        CertificateAuthority localCertificateAuthority = certificateAuthorityKeyStoreManager.getLocalCertificateAuthority();
        TlsToolkitClient tlsToolkitClient = new TlsToolkitClient(localCertificateAuthority);

        if (logger.isInfoEnabled()) {
            logger.info("Running standalone certificate generation with output directory " + baseDir);
        }

        NiFiPropertiesWriterFactory niFiPropertiesWriterFactory = standaloneConfig.getNiFiPropertiesWriterFactory();
        boolean overwrite = standaloneConfig.isOverwrite();

        List<InstanceDefinition> instanceDefinitions = standaloneConfig.getInstanceDefinitions();
        if (instanceDefinitions.isEmpty() && logger.isInfoEnabled()) {
            logger.info("No " + StandaloneCommandLine.HOSTNAMES_ARG + " specified, not generating any host certificates or configuration.");
        }
        for (InstanceDefinition instanceDefinition : instanceDefinitions) {
            String hostname = instanceDefinition.getHostname();
            File hostDir;
            int hostIdentifierNumber = instanceDefinition.getInstanceIdentifier().getNumber();
            if (hostIdentifierNumber == 1) {
                hostDir = new File(baseDir, hostname);
            } else {
                hostDir = new File(baseDir, hostname + "_" + hostIdentifierNumber);
            }

            CertificateAuthorityClientConfig certificateAuthorityClientConfig = new CertificateAuthorityClientConfig(standaloneConfig, instanceDefinition, hostDir);
            makeBaseDir(overwrite, hostDir, new File(certificateAuthorityClientConfig.getKeyStoreConfig().getKeyStore()),
                    new File(certificateAuthorityClientConfig.getTrustStoreConfig().getKeyStore()));

            tlsToolkitClient.getCertificates(certificateAuthorityClientConfig, hostDir, Arrays.asList(new NifiPropertiesConfigurationWriter(niFiPropertiesWriterFactory,
                    hostname, instanceDefinition.getNumber())));
            if (logger.isInfoEnabled()) {
                logger.info("Successfully generated TLS configuration for " + hostname + " " + hostIdentifierNumber + " in " + hostDir);
            }
        }

        List<String> clientDns = standaloneConfig.getClientDns();
        if (standaloneConfig.getClientDns().isEmpty() && logger.isInfoEnabled()) {
            logger.info("No " + StandaloneCommandLine.CLIENT_CERT_DN_ARG + " specified, not generating any client certificates.");
        }

        List<String> clientPasswords = standaloneConfig.getClientPasswords();
        for (int i = 0; i < clientDns.size(); i++) {
            String reorderedDn = CertificateUtils.reorderDn(clientDns.get(i));
            String clientDnFile = getClientDnFile(reorderedDn);
            File clientCertFile = new File(baseDir, clientDnFile + ".p12");

            if (clientCertFile.exists()) {
                if (overwrite) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Overwriting existing client cert " + clientCertFile);
                    }
                } else {
                    throw new IOException(clientCertFile + " exists and overwrite is not set.");
                }
            } else if (logger.isInfoEnabled()) {
                logger.info("Generating new client certificate " + clientCertFile);
            }
            KeyPair keyPair = TlsHelper.generateKeyPair(keyPairAlgorithm, keySize);
            X509Certificate[] x509Certificates = localCertificateAuthority.sign(TlsHelper.generateCertificationRequest(reorderedDn, keyPair, signingAlgorithm));
            X509Certificate clientCert = x509Certificates[0];
            KeyStore keyStore = KeyStore.getInstance(BaseTlsManager.PKCS_12, BouncyCastleProvider.PROVIDER_NAME);
            keyStore.load(null, null);
            keyStore.setKeyEntry(NIFI_KEY, keyPair.getPrivate(), null, new Certificate[]{clientCert, x509Certificates[1]});
            String password = TlsHelper.writeKeyStore(keyStore, outputStreamFactory, clientCertFile, clientPasswords.get(i), standaloneConfig.isClientPasswordsGenerated());

            try (FileWriter fileWriter = new FileWriter(new File(baseDir, clientDnFile + ".password"))) {
                fileWriter.write(password);
            }

            if (logger.isInfoEnabled()) {
                logger.info("Successfully generated client certificate " + clientCertFile);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("tls-toolkit standalone completed successfully");
        }
    }

    private void makeBaseDir(boolean overwrite, File hostDir, File keystore, File truststore) throws IOException {
        if (hostDir.exists()) {
            if (!hostDir.isDirectory()) {
                throw new IOException(hostDir + " exists but is not a directory.");
            } else if (overwrite) {
                if (logger.isInfoEnabled()) {
                    logger.info("Overwriting any existing ssl configuration in " + hostDir);
                }
                keystore.delete();
                if (keystore.exists()) {
                    throw new IOException("Keystore " + keystore + " already exists and couldn't be deleted.");
                }
                truststore.delete();
                if (truststore.exists()) {
                    throw new IOException("Truststore " + truststore + " already exists and couldn't be deleted.");
                }
            } else {
                throw new IOException(hostDir + " exists and overwrite is not set.");
            }
        } else if (!hostDir.mkdirs()) {
            throw new IOException("Unable to make directory: " + hostDir.getAbsolutePath());
        } else if (logger.isInfoEnabled()) {
            logger.info("Writing new ssl configuration to " + hostDir);
        }
    }
}
