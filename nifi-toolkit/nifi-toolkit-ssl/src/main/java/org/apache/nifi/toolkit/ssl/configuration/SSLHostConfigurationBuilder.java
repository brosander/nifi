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

package org.apache.nifi.toolkit.ssl.configuration;

import org.apache.nifi.toolkit.ssl.util.OutputStreamFactory;
import org.apache.nifi.toolkit.ssl.util.SSLHelper;
import org.apache.nifi.toolkit.ssl.properties.NiFiPropertiesWriterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class SSLHostConfigurationBuilder {
    private final SSLHelper sslHelper;
    private final NiFiPropertiesWriterFactory niFiPropertiesWriterFactory;

    private OutputStreamFactory outputStreamFactory = FileOutputStream::new;

    private File hostDir;
    private String httpsPort;
    private KeyPair certificateKeypair;
    private X509Certificate x509Certificate;
    private String keyStorePassword;
    private String keyPassword;
    private String trustStorePassword;
    private KeyStore trustStore;
    private String hostname;

    public SSLHostConfigurationBuilder(SSLHelper sslHelper, NiFiPropertiesWriterFactory niFiPropertiesWriterFactory) {
        this.sslHelper = sslHelper;
        this.niFiPropertiesWriterFactory = niFiPropertiesWriterFactory;
    }

    public SSLHostConfigurationBuilder setHostDir(File hostDir) {
        this.hostDir = hostDir;
        return this;
    }

    public SSLHostConfigurationBuilder setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
        return this;
    }

    public SSLHostConfigurationBuilder setCertificateKeypair(KeyPair certificateKeypair) {
        this.certificateKeypair = certificateKeypair;
        return this;
    }

    public SSLHostConfigurationBuilder setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        return this;
    }

    public SSLHostConfigurationBuilder setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public SSLHostConfigurationBuilder setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public SSLHostConfigurationBuilder setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public SSLHostConfigurationBuilder setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public SSLHostConfigurationBuilder setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    protected SSLHostConfigurationBuilder setOutputStreamFactory(OutputStreamFactory outputStreamFactory) {
        this.outputStreamFactory = outputStreamFactory;
        return this;
    }

    public SSLHostConfiguration createSSLHostConfiguration() {
        return new SSLHostConfiguration(outputStreamFactory, sslHelper, niFiPropertiesWriterFactory, hostDir, httpsPort, "." + sslHelper.getKeyStoreType(), certificateKeypair,
                x509Certificate, keyStorePassword, keyPassword, trustStorePassword, trustStore, hostname);
    }
}