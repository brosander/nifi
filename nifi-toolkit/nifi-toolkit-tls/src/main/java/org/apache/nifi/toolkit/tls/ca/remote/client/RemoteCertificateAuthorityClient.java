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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.nifi.toolkit.tls.ca.CertificateAuthority;
import org.apache.nifi.toolkit.tls.ca.remote.dto.RemoteCertificateAuthorityRequest;
import org.apache.nifi.toolkit.tls.ca.remote.dto.RemoteCertificateAuthorityResponse;
import org.apache.nifi.toolkit.tls.util.TlsHelper;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RemoteCertificateAuthorityClient implements CertificateAuthority {
    public static final String RECEIVED_RESPONSE_CODE = "Received response code ";
    public static final String EXPECTED_ONE_CERTIFICATE_CHAIN = "Expected one certificate chain";
    public static final String EXPECTED_RESPONSE_TO_CONTAIN_HMAC = "Expected response to contain hmac";
    public static final String UNEXPECTED_HMAC_RECEIVED_POSSIBLE_MAN_IN_THE_MIDDLE = "Unexpected hmac received, possible man in the middle";
    public static final String EXPECTED_RESPONSE_TO_CONTAIN_CERTIFICATE = "Expected response to contain certificate";

    private final Logger logger = LoggerFactory.getLogger(RemoteCertificateAuthorityClient.class);

    private final Supplier<HttpClientBuilder> httpClientBuilderSupplier;
    private final String caHostname;
    private final int port;
    private final String token;
    private final ObjectMapper objectMapper;

    public RemoteCertificateAuthorityClient(String caHostname, int port, String token) {
        this(caHostname, port, token, new ObjectMapper());
    }

    public RemoteCertificateAuthorityClient(String caHostname, int port, String token, ObjectMapper objectMapper) {
        this(HttpClientBuilder::create, caHostname, port, token, objectMapper);
    }

    public RemoteCertificateAuthorityClient(Supplier<HttpClientBuilder> httpClientBuilderSupplier, String caHostname, int port, String token, ObjectMapper objectMapper) {
        this.httpClientBuilderSupplier = httpClientBuilderSupplier;
        this.caHostname = caHostname;
        this.port = port;
        this.token = token;
        this.objectMapper = objectMapper;
    }

    @Override
    public X509Certificate[] sign(JcaPKCS10CertificationRequest certificationRequest) throws GeneralSecurityException, IOException {
        try {
            List<X509Certificate[]> certificateChains = new ArrayList<>();

            HttpClientBuilder httpClientBuilder = httpClientBuilderSupplier.get();
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.useProtocol("TLSv1.2");

            // We will be validating that we are talking to the correct host once we get the response's hmac of the token and public key of the ca
            sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            httpClientBuilder.setSSLSocketFactory(new RemoteCertificateAuthorityClientSocketFactory(sslContextBuilder.build(), caHostname, certificateChains));

            String jsonResponseString;
            int responseCode;
            try (CloseableHttpClient client = httpClientBuilder.build()) {
                RemoteCertificateAuthorityRequest tlsCertificateAuthorityRequest = new RemoteCertificateAuthorityRequest(TlsHelper.calculateHMac(token, certificationRequest.getPublicKey()),
                        TlsHelper.pemEncodeJcaObject(certificationRequest));

                HttpPost httpPost = new HttpPost();
                httpPost.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(tlsCertificateAuthorityRequest)));

                if (logger.isInfoEnabled()) {
                    logger.info("Requesting certificate with dn " + certificationRequest.getSubject().toString() + " from " + caHostname + ":" + port);
                }
                try (CloseableHttpResponse response = client.execute(new HttpHost(caHostname, port, "https"), httpPost)) {
                    jsonResponseString = IOUtils.toString(new BoundedInputStream(response.getEntity().getContent(), 1024 * 1024), StandardCharsets.UTF_8);
                    responseCode = response.getStatusLine().getStatusCode();
                }
            }

            if (responseCode != Response.SC_OK) {
                throw new IOException(RECEIVED_RESPONSE_CODE + responseCode + " with payload " + jsonResponseString);
            }

            if (certificateChains.size() != 1) {
                throw new IOException(EXPECTED_ONE_CERTIFICATE_CHAIN);
            }

            RemoteCertificateAuthorityResponse tlsCertificateAuthorityResponse = objectMapper.readValue(jsonResponseString, RemoteCertificateAuthorityResponse.class);
            if (!tlsCertificateAuthorityResponse.hasHmac()) {
                throw new IOException(EXPECTED_RESPONSE_TO_CONTAIN_HMAC);
            }

            X509Certificate[] caCertificateChain = certificateChains.get(0);
            X509Certificate caCertificate = caCertificateChain[0];
            byte[] expectedHmac = TlsHelper.calculateHMac(token, caCertificate.getPublicKey());

            if (!MessageDigest.isEqual(expectedHmac, tlsCertificateAuthorityResponse.getHmac())) {
                throw new GeneralSecurityException(UNEXPECTED_HMAC_RECEIVED_POSSIBLE_MAN_IN_THE_MIDDLE);
            }

            if (!tlsCertificateAuthorityResponse.hasCertificate()) {
                throw new IOException(EXPECTED_RESPONSE_TO_CONTAIN_CERTIFICATE);
            }
            X509Certificate x509Certificate = TlsHelper.parseCertificate(new StringReader(tlsCertificateAuthorityResponse.getPemEncodedCertificate()));
            x509Certificate.verify(caCertificate.getPublicKey());
            if (logger.isInfoEnabled()) {
                logger.info("Got certificate with dn " + x509Certificate.getSubjectX500Principal());
            }
            X509Certificate[] certificateChain = new X509Certificate[caCertificateChain.length + 1];
            certificateChain[0] = x509Certificate;
            System.arraycopy(caCertificateChain, 0, certificateChain, 1, certificateChain.length);
            return certificateChain;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof GeneralSecurityException) {
                throw (GeneralSecurityException) e;
            }
            throw new IOException(e);
        }
    }
}
