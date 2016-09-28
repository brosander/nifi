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

package org.apache.nifi.toolkit.tls.ca.local;

import org.apache.nifi.security.util.CertificateUtils;
import org.apache.nifi.toolkit.tls.ca.CertificateAuthority;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class LocalCertificateAuthority implements CertificateAuthority {
    private final X509Certificate[] caCertChain;
    private final KeyPair keyPair;
    private final String signingAlgorithm;
    private final int days;

    public LocalCertificateAuthority(X509Certificate[] caCertChain, KeyPair keyPair, String signingAlgorithm, int days) {
        this.caCertChain = caCertChain;
        this.keyPair = keyPair;
        this.signingAlgorithm = signingAlgorithm;
        this.days = days;
    }

    @Override
    public X509Certificate[] sign(JcaPKCS10CertificationRequest certificationRequest) throws GeneralSecurityException {
        X509Certificate signedCertificate = CertificateUtils.generateIssuedCertificate(certificationRequest.getSubject().toString(), certificationRequest.getPublicKey(), caCertChain[0], keyPair, signingAlgorithm, days);
        X509Certificate[] signedCertificateChain = new X509Certificate[caCertChain.length + 1];
        signedCertificateChain[0] = signedCertificate;
        System.arraycopy(caCertChain, 0, signedCertificateChain, 1, caCertChain.length);
        return signedCertificateChain;
    }
}
