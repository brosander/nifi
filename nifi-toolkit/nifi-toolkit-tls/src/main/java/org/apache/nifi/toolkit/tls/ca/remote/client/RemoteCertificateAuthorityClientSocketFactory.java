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

package org.apache.nifi.toolkit.tls.ca.remote.client;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Socket Factory validates that it is talking to a Ca claiming to have the given hostname.  It adds the certificate
 * to a list for later validation against the payload's hmac
 */
public class RemoteCertificateAuthorityClientSocketFactory extends SSLConnectionSocketFactory {
    private final String caHostname;
    private final List<X509Certificate[]> certificates;

    public RemoteCertificateAuthorityClientSocketFactory(SSLContext sslContext, String caHostname, List<X509Certificate[]> certificates) {
        super(sslContext);
        this.caHostname = caHostname;
        this.certificates = certificates;
    }

    @Override
    public synchronized Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                             InetSocketAddress localAddress, HttpContext context) throws IOException {
        Socket result = super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        if (!SSLSocket.class.isInstance(result)) {
            throw new IOException("Expected tls socket");
        }
        SSLSocket sslSocket = (SSLSocket) result;
        java.security.cert.Certificate[] peerCertificateChain = sslSocket.getSession().getPeerCertificates();
        if (peerCertificateChain == null || peerCertificateChain.length == 0) {
            throw new IOException("Expected at least one certificate in peer certificate chain");
        }
        X509Certificate[] x509Chain = new X509Certificate[peerCertificateChain.length];
        for (int i = 0; i < peerCertificateChain.length; i++) {
            if (!X509Certificate.class.isInstance(peerCertificateChain[i])) {
                throw new IOException("Expected certificate chain in X509 format");
            }
            x509Chain[i] = (X509Certificate) peerCertificateChain[i];
        }
        String cn;
        try {
            cn = IETFUtils.valueToString(new JcaX509CertificateHolder(x509Chain[0]).getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue());
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (!caHostname.equals(cn)) {
            throw new IOException("Expected cn of " + caHostname + " but got " + cn);
        }
        this.certificates.add(x509Chain);
        return result;
    }
}
