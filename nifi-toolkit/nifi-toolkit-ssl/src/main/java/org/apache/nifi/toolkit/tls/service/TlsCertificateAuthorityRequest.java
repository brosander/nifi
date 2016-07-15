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
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class TlsCertificateAuthorityRequest {
    private byte[] hmac;
    private String csr;

    public TlsCertificateAuthorityRequest() {
    }

    public TlsCertificateAuthorityRequest(byte[] hmac, JcaPKCS10CertificationRequest csr) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException {
        this.hmac = hmac;
        StringWriter writer = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(writer)) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(csr));
        }
        this.csr = writer.toString();
    }

    public byte[] getHmac() {
        return hmac;
    }

    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    public boolean hasHmac() {
        return hmac != null && hmac.length > 0;
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public boolean hasCsr() {
        return !StringUtils.isEmpty(csr);
    }

    public JcaPKCS10CertificationRequest parseCsr() throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(csr))) {
            Object o = pemParser.readObject();
            if (!PKCS10CertificationRequest.class.isInstance(o)) {
                throw new IOException("Expecting instance of " + PKCS10CertificationRequest.class + " but got " + o);
            }
            return new JcaPKCS10CertificationRequest((PKCS10CertificationRequest) o);
        }
    }
}
