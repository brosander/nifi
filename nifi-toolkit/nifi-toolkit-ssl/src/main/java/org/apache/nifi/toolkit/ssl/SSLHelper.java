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

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SSLHelper {
    public static final String PROVIDER = "BC";
    private final SecureRandom secureRandom;
    private final KeyPairGenerator keyPairGenerator;
    private final int days;
    private final String signingAlgorithm;
    private final String keyStoreType;

    public SSLHelper(int days, int keySize, String keyPairAlgorithm, String signingAlgorithm, String keyStoreType) throws NoSuchAlgorithmException {
        this(new SecureRandom(), createKeyPairGenerator(keyPairAlgorithm, keySize), days, signingAlgorithm, keyStoreType);
    }

    protected SSLHelper(SecureRandom secureRandom, KeyPairGenerator keyPairGenerator, int days, String signingAlgorithm, String keyStoreType) {
        this.secureRandom = secureRandom;
        this.keyPairGenerator = keyPairGenerator;
        this.days = days;
        this.signingAlgorithm = signingAlgorithm;
        this.keyStoreType = keyStoreType;
    }

    private static KeyPairGenerator createKeyPairGenerator(String algorithm, int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator instance = KeyPairGenerator.getInstance(algorithm);
        instance.initialize(keySize);
        return instance;
    }

    public String generatePassword() {
        // [see http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string#answer-41156]
        return new BigInteger(130, secureRandom).toString(32);
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return keyPairGenerator.generateKeyPair();
    }

    public void addToKeyStore(KeyStore keyStore, KeyPair keyPair, String alias, char[] passphrase, Certificate... certificates) throws GeneralSecurityException, IOException {
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), passphrase, certificates);
    }

    public KeyStore createKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        return keyStore;
    }

    public X509Certificate generateSelfSignedX509Certificate(KeyPair keyPair, String dn) throws OperatorCreationException, CertIOException, CertificateException, NoSuchAlgorithmException {
        ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(PROVIDER).build(keyPair.getPrivate());
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(days));

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name(dn),
                BigInteger.valueOf(System.currentTimeMillis()),
                startDate, endDate,
                new X500Name(dn),
                subPubKeyInfo);

        // Set certificate extensions
        // (1) digitalSignature extension
        certBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.nonRepudiation | KeyUsage.cRLSign | KeyUsage.keyCertSign));

        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

        // (2) extendedKeyUsage extension
        certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

        // Sign the certificate
        X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
        return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certificateHolder);
    }

    public X509Certificate generateIssuedCertificate(String dn, KeyPair keyPair, X509Certificate issuer, KeyPair issuerKeyPair) throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, SignatureException, InvalidKeyException, OperatorCreationException {
        ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(PROVIDER).build(issuerKeyPair.getPrivate());
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(days));

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer.getSubjectDN().getName()),
                BigInteger.valueOf(System.currentTimeMillis()),
                startDate, endDate,
                new X500Name(dn),
                subPubKeyInfo);

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerKeyPair.getPublic()));
        // Set certificate extensions
        // (1) digitalSignature extension
        certBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.nonRepudiation));

        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

        // (2) extendedKeyUsage extension
        certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

        X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
        return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certificateHolder);
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }
}
