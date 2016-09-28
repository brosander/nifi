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

package org.apache.nifi.toolkit.tls.keystore;

import org.apache.nifi.security.util.CertificateUtils;
import org.apache.nifi.toolkit.tls.ca.local.LocalCertificateAuthority;
import org.apache.nifi.toolkit.tls.configuration.CertificateAuthorityConfig;
import org.apache.nifi.toolkit.tls.configuration.CertificateGenerationConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.apache.nifi.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class CertificateAuthorityKeyStoreManager extends KeyStoreManager {
    private final Logger logger = LoggerFactory.getLogger(CertificateAuthorityKeyStoreManager.class);

    private final CertificateAuthorityConfig certificateAuthorityConfig;

    public CertificateAuthorityKeyStoreManager(CertificateAuthorityConfig certificateAuthorityConfig, File baseDir) throws GeneralSecurityException, IOException {
        this(certificateAuthorityConfig, baseDir, new PasswordUtil(), FileInputStream::new, FileOutputStream::new);
    }

    public CertificateAuthorityKeyStoreManager(CertificateAuthorityConfig certificateAuthorityConfig, File baseDir, PasswordUtil passwordUtil, InputStreamFactory inputStreamFactory,
                                               OutputStreamFactory outputStreamFactory) throws GeneralSecurityException, IOException {
        super(certificateAuthorityConfig.getKeyStoreConfig(), baseDir, passwordUtil, inputStreamFactory, outputStreamFactory);
        this.certificateAuthorityConfig = certificateAuthorityConfig;
        generateCertificateIfMissing(baseDir);
    }

    private void generateCertificateIfMissing(File baseDir) throws GeneralSecurityException, IOException {
        File keyStoreFile = new File(baseDir, certificateAuthorityConfig.getKeyStoreConfig().getKeyStore());
        CertificateGenerationConfig certificateGenerationConfig = certificateAuthorityConfig.getCertificateGenerationConfig();
        String keyStoreAlias = certificateGenerationConfig.getKeyStoreAlias();
        KeyStore.PrivateKeyEntry entry = getEntry(KeyStore.PrivateKeyEntry.class, keyStoreAlias);
        if (entry == null) {
            String dn = certificateAuthorityConfig.getDn();
            logger.info("Generating new CA certificate with dn " + dn);
            String keyPairAlgorithm = certificateGenerationConfig.getKeyPairAlgorithm();
            if (StringUtils.isEmpty(dn) || StringUtils.isEmpty(keyPairAlgorithm)) {
                throw new GeneralSecurityException("Missing alias " + keyStoreAlias + " and configured not to generate if missing.");
            }
            KeyPair keyPair = TlsHelper.generateKeyPair(keyPairAlgorithm, certificateGenerationConfig.getKeySize());
            X509Certificate caCert = CertificateUtils.generateSelfSignedX509Certificate(keyPair, CertificateUtils.reorderDn(dn), certificateGenerationConfig.getSigningAlgorithm(),
                    certificateAuthorityConfig.getDays());
            addPrivateKeyToKeyStore(keyPair, keyStoreAlias, caCert);
            storeKeystore(keyStoreFile);
        } else {
            X509Certificate[] x509CertificateChain = getX509CertificateChain(entry);
            logger.info("Using existing CA certificate with dn " + x509CertificateChain[0].getSubjectDN());
        }
    }

    private X509Certificate[] getX509CertificateChain(KeyStore.PrivateKeyEntry privateKeyEntry) throws GeneralSecurityException {
        KeyStore.PrivateKeyEntry entry = getEntry(KeyStore.PrivateKeyEntry.class, certificateAuthorityConfig.getCertificateGenerationConfig().getKeyStoreAlias());
        Certificate[] certificateChain = entry.getCertificateChain();
        X509Certificate[] x509CertificateChain = new X509Certificate[certificateChain.length];
        for (int i = 0; i < certificateChain.length; i++) {
            Certificate certificate = certificateChain[i];
            if (!X509Certificate.class.isInstance(certificate)) {
                throw new GeneralSecurityException("Expected certificate chain to consist of X509 certificates but found " + certificate);
            }
            x509CertificateChain[i] = (X509Certificate) certificate;
        }
        return x509CertificateChain;
    }

    public LocalCertificateAuthority getLocalCertificateAuthority() throws GeneralSecurityException {
        CertificateGenerationConfig certificateGenerationConfig = certificateAuthorityConfig.getCertificateGenerationConfig();
        KeyStore.PrivateKeyEntry entry = getEntry(KeyStore.PrivateKeyEntry.class, certificateGenerationConfig.getKeyStoreAlias());
        X509Certificate[] x509CertificateChain = getX509CertificateChain(entry);
        return new LocalCertificateAuthority(x509CertificateChain, new KeyPair(x509CertificateChain[0].getPublicKey(), entry.getPrivateKey()), certificateGenerationConfig.getSigningAlgorithm(),
                certificateAuthorityConfig.getDays());
    }
}
