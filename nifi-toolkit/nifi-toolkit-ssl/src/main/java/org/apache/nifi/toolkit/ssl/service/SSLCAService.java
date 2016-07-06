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

package org.apache.nifi.toolkit.ssl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.input.BoundedReader;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessage;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class SSLCAService extends AbstractHandler {
    public static final String CSR = "csr";
    public static final String HMAC = "hmac";
    public static final String CERTIFICATE = "certificate";
    public static final String ERROR = "error";
    private final KeyPair keyPair;
    private final X509Certificate caCert;
    private SSLHelper sslHelper;
    private String nonce;
    private String hostname;
    private List<String> hostnames;

    public SSLCAService() throws Exception {
        keyPair = sslHelper.generateKeyPair();
        caCert = sslHelper.generateSelfSignedX509Certificate(keyPair, "CN=" + hostname + ",OU=NIFI");
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.start();
        server.dumpStdErr();
        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(new BoundedReader(request.getReader(), 1024 * 1024));
            ObjectNode responseNode =  objectMapper.createObjectNode();
            JsonNode csrNode = jsonNode.get(CSR);
            String csrText;
            if (csrNode == null || StringUtils.isEmpty(csrText = csrNode.asText())) {
                responseNode.put(ERROR, "csr field must be set");
                writeResponse(objectMapper, response, responseNode, Response.SC_BAD_REQUEST);
                return;
            }

            JsonNode hmacNode = jsonNode.get(HMAC);
            String hmac;
            if (hmacNode == null || StringUtils.isEmpty(hmac = hmacNode.asText())) {
                responseNode.put(ERROR, "hmac field must be set");
                writeResponse(objectMapper, response, responseNode, Response.SC_BAD_REQUEST);
                return;
            }

            JcaCertificateRequestMessage csr = sslHelper.readCertificationRequest(new StringReader(csrText));

            if (sslHelper.checkHMac(hmac, nonce, csr.getPublicKey())) {
                StringWriter signedCertificate = new StringWriter();
                sslHelper.writeCertificate(sslHelper.signCsr(csr, this.caCert, keyPair), signedCertificate);

                responseNode.put(HMAC, Base64.getEncoder().encodeToString(sslHelper.calculateHMac(nonce, caCert.getPublicKey())));
                responseNode.put(CERTIFICATE, signedCertificate.toString());
                writeResponse(objectMapper, response, responseNode, Response.SC_OK);
                return;
            } else {
                responseNode.put(ERROR, "forbidden");
                writeResponse(objectMapper, response, responseNode, Response.SC_FORBIDDEN);
                return;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void writeResponse(ObjectMapper objectMapper, HttpServletResponse response, ObjectNode responseNode, int responseCode) throws IOException {
        objectMapper.writeTree(objectMapper.getFactory().createGenerator(response.getWriter()), responseNode);
        response.setStatus(responseCode);
    }
}
