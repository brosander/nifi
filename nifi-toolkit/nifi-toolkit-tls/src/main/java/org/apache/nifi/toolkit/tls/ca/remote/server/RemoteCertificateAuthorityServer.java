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

package org.apache.nifi.toolkit.tls.ca.remote.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.toolkit.tls.ca.CertificateAuthority;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PublicKey;

public class RemoteCertificateAuthorityServer {
    private final Logger logger = LoggerFactory.getLogger(RemoteCertificateAuthorityServer.class);

    private final OutputStreamFactory outputStreamFactory;

    private Server server;

    public RemoteCertificateAuthorityServer() {
        this(FileOutputStream::new);
    }

    public RemoteCertificateAuthorityServer(OutputStreamFactory outputStreamFactory) {
        this.outputStreamFactory = outputStreamFactory;
    }

    private static Server createServer(Handler handler, int port, KeyStore keyStore, String keyPassword) throws Exception {
        Server server = new Server();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setIncludeProtocols("TLSv1.2");
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyManagerPassword(keyPassword);

        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(port);

        server.addConnector(sslConnector);
        server.setHandler(handler);

        return server;
    }

    public synchronized void start(int port, CertificateAuthority delegate, PublicKey certificateAuthorityPublicKey, KeyStore keyStore, String keyPassword, String token) throws Exception {
        if (server != null) {
            throw new IllegalStateException("Server already started");
        }
        server = createServer(new RemoteCertificateAuthorityServiceHandler(delegate, certificateAuthorityPublicKey, token, new ObjectMapper()), port, keyStore, keyPassword);
        server.start();
    }

    public synchronized void shutdown() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Server already shutdown");
        }
        server.stop();
        server.join();
        server = null;
    }
}
