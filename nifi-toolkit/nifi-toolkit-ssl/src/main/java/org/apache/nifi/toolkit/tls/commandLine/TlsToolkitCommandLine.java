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

package org.apache.nifi.toolkit.tls.commandLine;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.nifi.toolkit.tls.TlsToolkitMain;
import org.apache.nifi.toolkit.tls.properties.NiFiPropertiesWriterFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.apache.nifi.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TlsToolkitCommandLine {
    public static final int HELP_EXIT_CODE = 1;
    public static final int ERROR_PARSING_COMMAND_LINE = 2;
    public static final int ERROR_PARSING_INT_DAYS = 3;
    public static final int ERROR_PARSING_INT_KEYSIZE = 4;
    public static final int ERROR_GENERATING_CONFIG = 5;
    public static final int ERROR_SAME_KEY_AND_KEY_PASSWORD = 6;
    public static final int ERROR_INCORRECT_NUMBER_OF_PASSWORDS = 7;
    public static final int ERROR_READING_NIFI_PROPERTIES = 8;

    public static final String DAYS_ARG = "days";
    public static final String KEY_SIZE_ARG = "keySize";
    public static final String KEY_ALGORITHM_ARG = "keyAlgorithm";
    public static final String SIGNING_ALGORITHM_ARG = "signingAlgorithm";
    public static final String KEY_STORE_TYPE_ARG = "keyStoreType";
    public static final String OUTPUT_DIRECTORY_ARG = "outputDirectory";
    public static final String HELP_ARG = "help";
    public static final String NIFI_PROPERTIES_FILE_ARG = "nifiPropertiesFile";
    public static final String KEY_STORE_PASSWORD_ARG = "keyStorePassword";
    public static final String TRUST_STORE_PASSWORD_ARG = "trustStorePassword";
    public static final String KEY_PASSWORD_ARG = "keyPassword";
    public static final String SAME_KEY_AND_KEY_STORE_PASSWORD_ARG = "sameKeyAndKeyStorePassword";
    public static final String DEFAULT_HOSTNAMES = "localhost";
    public static final String HOSTNAMES_ARG = "hostnames";
    public static final String HTTPS_PORT_ARG = "httpsPort";

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


    private final Options options;
    private final PasswordUtil passwordUtil;
    private int days;
    private int keySize;
    private String keyAlgorithm;
    private String signingAlgorithm;
    private String keyStoreType;
    private File baseDir;
    private List<String> hostnames;
    private String httpsPort;
    private NiFiPropertiesWriterFactory niFiPropertiesWriterFactory;
    private List<String> keyStorePasswords;
    private List<String> keyPasswords;
    private List<String> trustStorePasswords;

    public TlsToolkitCommandLine(SecureRandom secureRandom) {
        this.passwordUtil = new PasswordUtil(secureRandom);
        options = new Options();
        options.addOption("h", HELP_ARG, false, "Print help and exit.");
        addOptionWithArg(options, "a", KEY_ALGORITHM_ARG, "Algorithm to use for generated keys.", DEFAULT_KEY_ALGORITHM);
        addOptionWithArg(options, "k", KEY_SIZE_ARG, "Number of bits for generated keys.", DEFAULT_KEYSIZE);
        addOptionWithArg(options, "s", SIGNING_ALGORITHM_ARG, "Algorithm to use for signing certificates.", DEFAULT_SIGNING_ALGORITHM);
        addOptionWithArg(options, "d", DAYS_ARG, "Number of days self signed certificate should be valid for.", DEFAULT_CERT_DAYS);
        addOptionWithArg(options, "t", KEY_STORE_TYPE_ARG, "The type of keyStores to generate.", DEFAULT_KEY_STORE_TYPE);
        addOptionWithArg(options, "o", OUTPUT_DIRECTORY_ARG, "The directory to output keystores, truststore, config files.", DEFAULT_OUTPUT_DIRECTORY);
        addOptionWithArg(options, "n", HOSTNAMES_ARG, "Comma separated list of hostnames.", DEFAULT_HOSTNAMES);
        addOptionWithArg(options, "p", HTTPS_PORT_ARG, "Https port to use.", "");
        addOptionWithArg(options, "f", NIFI_PROPERTIES_FILE_ARG, "Base nifi.properties file to update.", "");
        options.addOption("R", SAME_KEY_AND_KEY_STORE_PASSWORD_ARG, false, "Use the same password for KeyStore and Key, only KeyStore password should be specified if autogenerate not desired.");
        addOptionWithArg(options, "S", KEY_STORE_PASSWORD_ARG, "Keystore password to use.  Must either be one value or one for each host. (autogenerate if not specified)");
        addOptionWithArg(options, "K", KEY_PASSWORD_ARG, "Key password to use.  Must either be one value or one for each host. (autogenerate if not specified)");
        addOptionWithArg(options, "T", TRUST_STORE_PASSWORD_ARG, "Keystore password to use.  Must either be one value or one for each host. (autogenerate if not specified)");
    }

    private static void addOptionWithArg(Options options, String arg, String longArg, String description) {
        addOptionWithArg(options, arg, longArg, description, null);
    }

    private static void addOptionWithArg(Options options, String arg, String longArg, String description, Object defaultVal) {
        String fullDescription = description;
        if (defaultVal != null) {
            fullDescription += " (default: " + defaultVal + ")";
        }
        options.addOption(arg, longArg, true, fullDescription);
    }

    public void printUsage(String errorMessage) {
        if (errorMessage != null) {
            System.out.println(errorMessage);
            System.out.println();
        }
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(160);
        helpFormatter.printHelp(TlsToolkitMain.class.getCanonicalName(), HEADER, options, FOOTER, true);
    }

    public <T> T printUsageAndThrow(String errorMessage, int exitCode) throws CommandLineParseException {
        printUsage(errorMessage);
        throw new CommandLineParseException(errorMessage, exitCode);
    }

    public void parse(String... args) throws CommandLineParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsageAndThrow("Error parsing command line. (" + e.getMessage() + ")", ERROR_PARSING_COMMAND_LINE);
        }

        if (commandLine.hasOption(HELP_ARG)) {
            printUsageAndThrow(null, HELP_EXIT_CODE);
        }

        days = 0;
        try {
            days = Integer.parseInt(commandLine.getOptionValue(DAYS_ARG, DEFAULT_CERT_DAYS));
        } catch (NumberFormatException e) {
            printUsageAndThrow("Expected integer for days argument. (" + e.getMessage() + ")", ERROR_PARSING_INT_DAYS);
        }

        keySize = 0;
        try {
            keySize = Integer.parseInt(commandLine.getOptionValue(KEY_SIZE_ARG, DEFAULT_KEYSIZE));
        } catch (NumberFormatException e) {
            printUsageAndThrow("Expected integer for keySize argument. (" + e.getMessage() + ")", ERROR_PARSING_INT_KEYSIZE);
        }

        keyAlgorithm = commandLine.getOptionValue(KEY_ALGORITHM_ARG, DEFAULT_KEY_ALGORITHM);
        signingAlgorithm = commandLine.getOptionValue(SIGNING_ALGORITHM_ARG, DEFAULT_SIGNING_ALGORITHM);
        keyStoreType = commandLine.getOptionValue(KEY_STORE_TYPE_ARG, DEFAULT_KEY_STORE_TYPE);
        String outputDirectory = commandLine.getOptionValue(OUTPUT_DIRECTORY_ARG, DEFAULT_OUTPUT_DIRECTORY);
        baseDir = new File(outputDirectory);
        hostnames = Arrays.stream(commandLine.getOptionValue(HOSTNAMES_ARG, DEFAULT_HOSTNAMES).split(",")).map(String::trim).collect(Collectors.toList());
        httpsPort = commandLine.getOptionValue(HTTPS_PORT_ARG, "");

        int numHosts = hostnames.size();
        keyStorePasswords = Collections.unmodifiableList(getPasswords(KEY_STORE_PASSWORD_ARG, commandLine, numHosts));
        keyPasswords = Collections.unmodifiableList(getKeyPasswords(commandLine, keyStorePasswords));
        trustStorePasswords = Collections.unmodifiableList(getPasswords(TRUST_STORE_PASSWORD_ARG, commandLine, numHosts));

        String nifiPropertiesFile = commandLine.getOptionValue(NIFI_PROPERTIES_FILE_ARG, "");
        try {
            if (StringUtils.isEmpty(nifiPropertiesFile)) {
                niFiPropertiesWriterFactory = new NiFiPropertiesWriterFactory();
            } else {
                niFiPropertiesWriterFactory = new NiFiPropertiesWriterFactory(new FileInputStream(nifiPropertiesFile));
            }
        } catch (IOException e) {
            printUsageAndThrow("Unable to read nifi.properties from " + (StringUtils.isEmpty(nifiPropertiesFile) ? "classpath" : nifiPropertiesFile), ERROR_READING_NIFI_PROPERTIES);
        }
    }

    private List<String> getPasswords(String arg, CommandLine commandLine, int numHosts) throws CommandLineParseException {
        String[] optionValues = commandLine.getOptionValues(arg);
        if (optionValues == null) {
            return IntStream.range(0, numHosts).mapToObj(operand -> passwordUtil.generatePassword()).collect(Collectors.toList());
        }
        if (optionValues.length == 1) {
            return IntStream.range(0, numHosts).mapToObj(value -> optionValues[0]).collect(Collectors.toList());
        } else if (optionValues.length == numHosts) {
            return Arrays.stream(optionValues).collect(Collectors.toList());
        }
        return printUsageAndThrow("Expected either 1 value or " + numHosts + " (the number of hostnames) values for " + arg, ERROR_INCORRECT_NUMBER_OF_PASSWORDS);
    }

    private List<String> getKeyPasswords(CommandLine commandLine, List<String> keyStorePasswords) throws CommandLineParseException {
        if (commandLine.hasOption(SAME_KEY_AND_KEY_STORE_PASSWORD_ARG)) {
            if (commandLine.hasOption(KEY_PASSWORD_ARG)) {
                return printUsageAndThrow(SAME_KEY_AND_KEY_STORE_PASSWORD_ARG + " and " + KEY_PASSWORD_ARG + " arguments are mutually exclusive.", ERROR_SAME_KEY_AND_KEY_PASSWORD);
            }
            return new ArrayList<>(keyStorePasswords);
        }
        return getPasswords(KEY_PASSWORD_ARG, commandLine, keyStorePasswords.size());
    }

    public int getDays() {
        return days;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public List<String> getHostnames() {
        return hostnames;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public NiFiPropertiesWriterFactory getNiFiPropertiesWriterFactory() {
        return niFiPropertiesWriterFactory;
    }

    public List<String> getKeyStorePasswords() {
        return keyStorePasswords;
    }

    public List<String> getKeyPasswords() {
        return keyPasswords;
    }

    public List<String> getTrustStorePasswords() {
        return trustStorePasswords;
    }
}
