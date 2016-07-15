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

import org.apache.nifi.util.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TlsCertificateAuthorityResponse {
    private byte[] hmac;
    private String certificate;
    private String error;

    public TlsCertificateAuthorityResponse() {
    }

    public TlsCertificateAuthorityResponse(byte[] hmac, X509Certificate certificate) throws IOException {
        this.hmac = hmac;
        StringWriter writer = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(writer)) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(certificate));
        }
        this.certificate = writer.toString();
    }

    public TlsCertificateAuthorityResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public byte[] getHmac() {
        return hmac;
    }

    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public boolean hasCertificate() {
        return !StringUtils.isEmpty(certificate);
    }

    public boolean hasHmac() {
        return hmac != null && hmac.length > 0;
    }

    public X509Certificate parseCertificate() throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(new StringReader(certificate))) {
            Object object = pemParser.readObject();
            if (!X509CertificateHolder.class.isInstance(object)) {
                throw new IOException("Expected " + X509CertificateHolder.class);
            }
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate((X509CertificateHolder) object);
        }
    }
}
