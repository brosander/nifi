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

package org.apache.nifi.toolkit.tls.util;

import org.apache.nifi.toolkit.tls.commandLine.TlsToolkitCommandLine;
import org.apache.nifi.toolkit.tls.configuration.TlsHelperConfig;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.crmf.CRMFException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.eac.EACException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TlsHelper {
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    private final KeyPairGenerator keyPairGenerator;
    private final int days;
    private final String signingAlgorithm;
    private final String keyStoreType;

    public TlsHelper(TlsHelperConfig tlsHelperConfig) throws NoSuchAlgorithmException {
        this(tlsHelperConfig.getDays(), tlsHelperConfig.getKeySize(), tlsHelperConfig.getKeyPairAlgorithm(), tlsHelperConfig.getSigningAlgorithm(), tlsHelperConfig.getKeyStoreType());
    }

    public TlsHelper(TlsToolkitCommandLine tlsToolkitCommandLine) throws NoSuchAlgorithmException {
        this(tlsToolkitCommandLine.getDays(), tlsToolkitCommandLine.getKeySize(), tlsToolkitCommandLine.getKeyAlgorithm(),
                tlsToolkitCommandLine.getSigningAlgorithm(), tlsToolkitCommandLine.getKeyStoreType());
    }

    public TlsHelper(int days, int keySize, String keyPairAlgorithm, String signingAlgorithm, String keyStoreType) throws NoSuchAlgorithmException {
        this(createKeyPairGenerator(keyPairAlgorithm, keySize), days, signingAlgorithm, keyStoreType);
    }

    protected TlsHelper(KeyPairGenerator keyPairGenerator, int days, String signingAlgorithm, String keyStoreType) {
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
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment
                | KeyUsage.keyAgreement | KeyUsage.nonRepudiation | KeyUsage.cRLSign | KeyUsage.keyCertSign));

        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

        // (2) extendedKeyUsage extension
        certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));

        // Sign the certificate
        X509CertificateHolder certificateHolder = certBuilder.build(sigGen);
        return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certificateHolder);
    }

    public X509Certificate generateIssuedCertificate(String dn, KeyPair keyPair, X509Certificate issuer, KeyPair issuerKeyPair)
            throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, SignatureException, InvalidKeyException, OperatorCreationException {
        return generateIssuedCertificate(dn, keyPair.getPublic(), issuer, issuerKeyPair);
    }

    public X509Certificate generateIssuedCertificate(String dn, PublicKey publicKey, X509Certificate issuer, KeyPair issuerKeyPair)
            throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, SignatureException, InvalidKeyException, OperatorCreationException {
        return generateIssuedCertificate(new X500Name(dn), publicKey, issuer, issuerKeyPair);
    }

    public X509Certificate generateIssuedCertificate(X500Name dn, PublicKey publicKey, X509Certificate issuer, KeyPair issuerKeyPair)
            throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, SignatureException, InvalidKeyException, OperatorCreationException {
        ContentSigner sigGen = new JcaContentSignerBuilder(signingAlgorithm).setProvider(PROVIDER).build(issuerKeyPair.getPrivate());
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + TimeUnit.DAYS.toMillis(days));

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer.getSubjectDN().getName()),
                BigInteger.valueOf(System.currentTimeMillis()),
                startDate, endDate,
                dn,
                subPubKeyInfo);

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));

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

    public JcaPKCS10CertificationRequest generateCertificationRequest(String requestedDn, KeyPair keyPair) throws OperatorCreationException {
        JcaPKCS10CertificationRequestBuilder jcaPKCS10CertificationRequestBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Principal(requestedDn), keyPair.getPublic());
        JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder(signingAlgorithm);
        return new JcaPKCS10CertificationRequest(jcaPKCS10CertificationRequestBuilder.build(jcaContentSignerBuilder.build(keyPair.getPrivate())));
    }

    public X509Certificate signCsr(JcaPKCS10CertificationRequest certificationRequest, X509Certificate issuer, KeyPair issuerKeyPair) throws InvalidKeySpecException, EACException,
            CertificateException, NoSuchAlgorithmException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, OperatorCreationException, CRMFException {
        return generateIssuedCertificate(certificationRequest.getSubject(), certificationRequest.getPublicKey(), issuer, issuerKeyPair);
    }

    public X509Certificate readCertificate(Reader reader) throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(reader)) {
            Object object = pemParser.readObject();
            if (!X509CertificateHolder.class.isInstance(object)) {
                throw new IOException("Expected " + X509CertificateHolder.class);
            }
            return new JcaX509CertificateConverter().setProvider(TlsHelper.PROVIDER).getCertificate((X509CertificateHolder) object);
        }
    }

    public void writeCertificate(X509Certificate x509Certificate, Writer writer) throws IOException {
        try (PemWriter pemWriter = new PemWriter(writer)) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(x509Certificate));
        }
    }

    public boolean checkHMac(String hmac, String nonce, PublicKey publicKey) throws CRMFException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        return MessageDigest.isEqual(Base64.getDecoder().decode(hmac), calculateHMac(nonce, publicKey));
    }

    public boolean checkHMac(String hmac, String nonce, byte[] publicKeyFingerprint) throws CRMFException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        return MessageDigest.isEqual(Base64.getDecoder().decode(hmac), calculateHMac(nonce, publicKeyFingerprint));
    }

    public byte[] calculateHMac(String nonce, byte[] publicKeyFingerprint) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(nonce.getBytes(StandardCharsets.UTF_8), "RAW");
        Mac mac = Mac.getInstance("Hmac-SHA256", PROVIDER);
        mac.init(keySpec);
        return mac.doFinal(publicKeyFingerprint);
    }

    public byte[] calculateHMac(String nonce, PublicKey publicKey) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        return calculateHMac(nonce, getKeyIdentifier(publicKey));
    }

    public byte[] getKeyIdentifier(PublicKey publicKey) throws NoSuchAlgorithmException {
        return new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey).getKeyIdentifier();
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }
}
