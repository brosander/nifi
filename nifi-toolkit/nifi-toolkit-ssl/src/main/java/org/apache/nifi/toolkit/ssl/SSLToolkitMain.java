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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.util.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import sun.security.pkcs.PKCS8Key;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SSLToolkitMain {
    public static final int HELP_EXIT_CODE = 1;
    public static final int ERROR_PARSING_COMMAND_LINE = 2;
    public static final int ERROR_PARSING_INT_DAYS = 3;
    public static final int ERROR_PARSING_INT_KEYSIZE = 4;
    public static final int ERROR_GENERATING_CONFIG = 5;

    public static final String DAYS_ARG = "days";
    public static final String KEY_SIZE_ARG = "keySize";
    public static final String KEY_ALGORITHM_ARG = "keyAlgorithm";
    public static final String SIGNING_ALGORITHM_ARG = "signingAlgorithm";
    public static final String KEY_STORE_TYPE_ARG = "keyStoreType";
    public static final String OUTPUT_DIRECTORY_ARG = "outputDirectory";
    public static final String HELP_ARG = "help";

    public static final String DEFAULT_CERT_DAYS = "365";
    public static final String DEFAULT_KEYSIZE = "2048";
    public static final String DEFAULT_KEY_ALGORITHM = "RSA";
    public static final String DEFAULT_OUTPUT_DIRECTORY = new File(".").getAbsolutePath();
    public static final String DEFAULT_SIGNING_ALGORITHM = "SHA256WITHRSA";
    public static final String DEFAULT_KEY_STORE_TYPE = "jks";

    public static final String JAVA_HOME = "JAVA_HOME";
    public static final String NIFI_TOOLKIT_HOME = "NIFI_TOOLKIT_HOME";

    public static final String HEADER = new StringBuilder(System.lineSeparator()).append("Creates certificates and config files for nifi cluster.")
            .append(System.lineSeparator()).append(System.lineSeparator()).toString();
    public static final String FOOTER = new StringBuilder(System.lineSeparator()).append("Java home: ")
            .append(System.getenv(JAVA_HOME)).append(System.lineSeparator()).append("NiFi Toolkit home: ").append(System.getenv(NIFI_TOOLKIT_HOME)).toString();
    public static final String DEFAULT_HOSTNAMES = "localhost";
    public static final String HOSTNAMES_ARG = "hostnames";
    public static final String HTTPS_PORT_ARG = "httpsPort";
    public static final String NIFI_KEY = "nifi-key";
    public static final String NIFI_CERT = "nifi-cert";
    public static final String ROOT_CERT_PRIVATE_KEY = "rootCertPrivate.key";

    private final SSLHelper sslHelper;
    private final File baseDir;
    private final NifiPropertiesHelper nifiPropertiesHelper;

    public SSLToolkitMain(SSLHelper sslHelper, File baseDir, NifiPropertiesHelper nifiPropertiesHelper) {
        this.sslHelper = sslHelper;
        this.baseDir = baseDir;
        this.nifiPropertiesHelper = nifiPropertiesHelper;
    }

    private static void addOptionWithArg(Options options, String arg, String longArg, String description, Object defaultVal) {
        options.addOption(arg, longArg, true, description + " (default: " + defaultVal + ")");
    }

    private static void printUsageAndExit(String errorMessage, Options options, int exitCode) {
        if (errorMessage != null) {
            System.out.println(errorMessage);
            System.out.println();
        }
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(160);
        helpFormatter.printHelp(SSLToolkitMain.class.getCanonicalName(), HEADER, options, FOOTER, true);
        System.exit(exitCode);
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        Options options = new Options();
        options.addOption("h", HELP_ARG, false, "Print help and exit.");
        addOptionWithArg(options, "a", KEY_ALGORITHM_ARG, "Algorithm to use for generated keys.", DEFAULT_KEY_ALGORITHM);
        addOptionWithArg(options, "k", KEY_SIZE_ARG, "Number of bits for generated keys.", DEFAULT_KEYSIZE);
        addOptionWithArg(options, "s", SIGNING_ALGORITHM_ARG, "Algorithm to use for signing certificates.", DEFAULT_SIGNING_ALGORITHM);
        addOptionWithArg(options, "d", DAYS_ARG, "Number of days self signed certificate should be valid for.", DEFAULT_CERT_DAYS);
        addOptionWithArg(options, "t", KEY_STORE_TYPE_ARG, "The type of keyStores to generate.", DEFAULT_KEY_STORE_TYPE);
        addOptionWithArg(options, "o", OUTPUT_DIRECTORY_ARG, "The directory to output keystores, truststore, config files.", DEFAULT_OUTPUT_DIRECTORY);
        addOptionWithArg(options, "n", HOSTNAMES_ARG, "Comma separated list of hostnames.", DEFAULT_HOSTNAMES);
        addOptionWithArg(options, "p", HTTPS_PORT_ARG, "Https port to use.",  "");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsageAndExit("Error parsing command line. (" + e.getMessage() + ")", options, ERROR_PARSING_COMMAND_LINE);
        }

        if (commandLine.hasOption(HELP_ARG)) {
            printUsageAndExit(null, options, HELP_EXIT_CODE);
        }

        int days = 0;
        try {
            days = Integer.parseInt(commandLine.getOptionValue(DAYS_ARG, DEFAULT_CERT_DAYS));
        } catch (NumberFormatException e) {
            printUsageAndExit("Expected integer for days argument. (" + e.getMessage() + ")", options, ERROR_PARSING_INT_DAYS);
        }

        int keySize = 0;
        try {
            keySize = Integer.parseInt(commandLine.getOptionValue(KEY_SIZE_ARG, DEFAULT_KEYSIZE));
        } catch (NumberFormatException e) {
            printUsageAndExit("Expected integer for keySize argument. (" + e.getMessage() + ")", options, ERROR_PARSING_INT_KEYSIZE);
        }

        String keyAlgorithm = commandLine.getOptionValue(KEY_ALGORITHM_ARG, DEFAULT_KEY_ALGORITHM);

        String signingAlgorithm = commandLine.getOptionValue(SIGNING_ALGORITHM_ARG, DEFAULT_SIGNING_ALGORITHM);

        String keyStoreType = commandLine.getOptionValue(KEY_STORE_TYPE_ARG, DEFAULT_KEY_STORE_TYPE);

        String outputDirectory = commandLine.getOptionValue(OUTPUT_DIRECTORY_ARG, DEFAULT_OUTPUT_DIRECTORY);

        File baseDir = new File(outputDirectory);

        List<String> hostnames = Arrays.stream(commandLine.getOptionValue(HOSTNAMES_ARG, DEFAULT_HOSTNAMES).split(",")).map(String::trim).collect(Collectors.toList());

        String httpsPort = commandLine.getOptionValue(HTTPS_PORT_ARG, "");

        try {
            SSLHelper sslHelper = new SSLHelper(days, keySize, keyAlgorithm, signingAlgorithm, keyStoreType);
            new SSLToolkitMain(sslHelper, baseDir, new NifiPropertiesHelper()).createNifiKeystoresAndTrustStores("CN=nifi.root.ca,OU=apache.nifi", hostnames, httpsPort);
        } catch (Exception e) {
            printUsageAndExit("Error creating generating ssl configuration. (" + e.getMessage() + ")", options, ERROR_GENERATING_CONFIG);
        }
        System.exit(0);
    }

    public void createNifiKeystoresAndTrustStores(String dn, List<String> hostnames, String httpsPort) throws GeneralSecurityException, IOException, OperatorCreationException {
        String extension = "." + sslHelper.getKeyStoreType().toLowerCase();

        KeyPair certificateKeypair = sslHelper.generateKeyPair();
        X509Certificate x509Certificate = sslHelper.generateSelfSignedX509Certificate(certificateKeypair, dn);

        String trustStoreName = "truststore" + extension;
        String trustStorePassword = sslHelper.generatePassword();

        KeyStore trustStore = sslHelper.createKeyStore();
        trustStore.setCertificateEntry(NIFI_CERT, x509Certificate);

        for (String hostname : hostnames) {
            processHost(httpsPort, extension, certificateKeypair, x509Certificate, trustStoreName, trustStorePassword, trustStore, hostname);
        }
    }

    private void processHost(String httpsPort, String extension, KeyPair certificateKeypair, X509Certificate x509Certificate, String trustStoreName, String trustStorePassword, KeyStore trustStore, String hostname) throws IOException, GeneralSecurityException, OperatorCreationException {
        String keyPassword = sslHelper.generatePassword();
        String keyStorePassword = sslHelper.generatePassword();
        KeyPair keyPair = sslHelper.generateKeyPair();

        KeyStore keyStore = sslHelper.createKeyStore();
        sslHelper.addToKeyStore(keyStore, keyPair, NIFI_KEY, keyPassword.toCharArray(),
                sslHelper.generateIssuedCertificate("CN=" + hostname + ",OU=apache.nifi", keyPair, x509Certificate, certificateKeypair), x509Certificate);

        String keyStoreName = hostname + extension;

        File hostDir = new File(baseDir, hostname);
        if (!hostDir.mkdirs()) {
            throw new IOException("Unable to make directory: " + hostDir.getAbsolutePath());
        }

        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
        try (OutputStream outputStream = new FileOutputStream(new File(hostDir, ROOT_CERT_PRIVATE_KEY))) {
            outputStream.write(pkcs8EncodedKeySpec.getEncoded());
        }

        File propertiesFile = new File(hostDir, "nifi.properties");

        Map<String, String> updatedProperties = new HashMap<>();
        updatedProperties.put(NiFiProperties.SECURITY_KEYSTORE, "./conf/" + keyStoreName);
        updatedProperties.put(NiFiProperties.SECURITY_KEYSTORE_TYPE, sslHelper.getKeyStoreType());
        updatedProperties.put(NiFiProperties.SECURITY_KEYSTORE_PASSWD, keyStorePassword);
        updatedProperties.put(NiFiProperties.SECURITY_KEY_PASSWD, keyPassword);
        updatedProperties.put(NiFiProperties.SECURITY_TRUSTSTORE, "./conf/truststore" + extension);
        updatedProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_TYPE, sslHelper.getKeyStoreType());
        updatedProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD, trustStorePassword);
        if (!StringUtils.isEmpty(httpsPort)) {
            updatedProperties.put(NiFiProperties.WEB_HTTPS_PORT, httpsPort);
            updatedProperties.put(NiFiProperties.WEB_HTTP_PORT, "");
            updatedProperties.put(NiFiProperties.SITE_TO_SITE_SECURE, "true");
        }

        try (OutputStream outputStream = new FileOutputStream(propertiesFile)) {
            nifiPropertiesHelper.outputWithUpdatedPropertyValues(outputStream, updatedProperties);
        }

        File keyStoreFile = new File(hostDir, keyStoreName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fileOutputStream, keyStorePassword.toCharArray());
        }

        File trustStoreFile = new File(hostDir, trustStoreName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(trustStoreFile)) {
            trustStore.store(fileOutputStream, trustStorePassword.toCharArray());
        }
    }
}
