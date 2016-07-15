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

package org.apache.nifi.toolkit.ssl.commandLine;

import org.apache.nifi.toolkit.ssl.TlsToolkitMainTest;
import org.apache.nifi.toolkit.ssl.properties.NiFiPropertiesWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.stubbing.defaultanswers.ForwardsInvocations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class SSLToolkitCommandLineTest {
    private SecureRandom secureRandom;
    private SSLToolkitCommandLine sslToolkitCommandLine;

    @Before
    public void setup() {
        secureRandom = mock(SecureRandom.class);
        doAnswer(new ForwardsInvocations(new Random())).when(secureRandom).nextBytes(any(byte[].class));
        sslToolkitCommandLine = new SSLToolkitCommandLine(secureRandom);
    }

    @Test
    public void testHelp() {
        try {
            sslToolkitCommandLine.parse("-h");
            fail("Expected usage and help exit");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.HELP_EXIT_CODE, e.getExitCode());
        }
    }

    @Test
    public void testUnknownArg() {
        try {
            sslToolkitCommandLine.parse("--unknownArg");
            fail("Expected error parsing command line");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_PARSING_COMMAND_LINE, e.getExitCode());
        }
    }

    @Test
    public void testKeyAlgorithm() throws CommandLineParseException {
        String testKeyAlgorithm = "testKeyAlgorithm";
        sslToolkitCommandLine.parse("-a", testKeyAlgorithm);
        assertEquals(testKeyAlgorithm, sslToolkitCommandLine.getKeyAlgorithm());
    }

    @Test
    public void testKeySizeArgNotInteger() {
        try {
            sslToolkitCommandLine.parse("-k", "badVal");
            fail("Expected bad keysize exit code");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_PARSING_INT_KEYSIZE, e.getExitCode());
        }
    }

    @Test
    public void testKeySize() throws CommandLineParseException {
        int testKeySize = 4096;
        sslToolkitCommandLine.parse("-k", Integer.toString(testKeySize));
        assertEquals(testKeySize, sslToolkitCommandLine.getKeySize());
    }

    @Test
    public void testSigningAlgorithm() throws CommandLineParseException {
        String testSigningAlgorithm = "testSigningAlgorithm";
        sslToolkitCommandLine.parse("-s", testSigningAlgorithm);
        assertEquals(testSigningAlgorithm, sslToolkitCommandLine.getSigningAlgorithm());
    }

    @Test
    public void testDaysNotInteger() {
        try {
            sslToolkitCommandLine.parse("-d", "badVal");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_PARSING_INT_DAYS, e.getExitCode());
        }
    }

    @Test
    public void testDays() throws CommandLineParseException {
        int testDays = 29;
        sslToolkitCommandLine.parse("-d", Integer.toString(testDays));
        assertEquals(testDays, sslToolkitCommandLine.getDays());
    }

    @Test
    public void testKeyStoreType() throws CommandLineParseException {
        String testKeyStoreType = "testKeyStoreType";
        sslToolkitCommandLine.parse("-t", testKeyStoreType);
        assertEquals(testKeyStoreType, sslToolkitCommandLine.getKeyStoreType());
    }

    @Test
    public void testOutputDirectory() throws CommandLineParseException {
        String testPath = "/fake/path/doesnt/exist";
        sslToolkitCommandLine.parse("-o", testPath);
        assertEquals(testPath, sslToolkitCommandLine.getBaseDir().getAbsolutePath());
    }

    @Test
    public void testHostnames() throws CommandLineParseException {
        String nifi1 = "nifi1";
        String nifi2 = "nifi2";

        sslToolkitCommandLine.parse("-n", nifi1 + " , " + nifi2);

        List<String> hostnames = sslToolkitCommandLine.getHostnames();
        assertEquals(2, hostnames.size());
        assertEquals(nifi1, hostnames.get(0));
        assertEquals(nifi2, hostnames.get(1));
    }

    @Test
    public void testHttpsPort() throws CommandLineParseException {
        String testPort = "8998";
        sslToolkitCommandLine.parse("-p", testPort);
        assertEquals(testPort, sslToolkitCommandLine.getHttpsPort());
    }

    @Test
    public void testNifiPropertiesFile() throws CommandLineParseException, IOException {
        sslToolkitCommandLine.parse("-f", TlsToolkitMainTest.TEST_NIFI_PROPERTIES);
        assertEquals(TlsToolkitMainTest.FAKE_VALUE, getProperties().get(TlsToolkitMainTest.NIFI_FAKE_PROPERTY));
    }

    @Test
    public void testNifiPropertiesFileDefault() throws CommandLineParseException, IOException {
        sslToolkitCommandLine.parse();
        assertNull(getProperties().get(TlsToolkitMainTest.NIFI_FAKE_PROPERTY));
    }

    @Test
    public void testBadNifiPropertiesFile() {
        try {
            sslToolkitCommandLine.parse("-f", "/this/file/should/not/exist.txt");
            fail("Expected error when unable to read file");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_READING_NIFI_PROPERTIES, e.getExitCode());
        }
    }

    @Test
    public void testNotSameKeyAndKeystorePassword() throws CommandLineParseException {
        sslToolkitCommandLine.parse();
        List<String> keyStorePasswords = sslToolkitCommandLine.getKeyStorePasswords();
        List<String> keyPasswords = sslToolkitCommandLine.getKeyPasswords();
        assertEquals(1, sslToolkitCommandLine.getHostnames().size());
        assertEquals(1, keyStorePasswords.size());
        assertEquals(1, keyPasswords.size());
        assertNotEquals(keyStorePasswords.get(0), keyPasswords.get(0));
    }

    @Test
    public void testSameKeyAndKeystorePassword() throws CommandLineParseException {
        sslToolkitCommandLine.parse("-R");
        List<String> keyStorePasswords = sslToolkitCommandLine.getKeyStorePasswords();
        List<String> keyPasswords = sslToolkitCommandLine.getKeyPasswords();
        assertEquals(1, sslToolkitCommandLine.getHostnames().size());
        assertEquals(1, keyStorePasswords.size());
        assertEquals(1, keyPasswords.size());
        assertEquals(keyStorePasswords.get(0), keyPasswords.get(0));
    }

    @Test
    public void testSameKeyAndKeystorePasswordWithKeystorePasswordSpecified() throws CommandLineParseException {
        String testPassword = "testPassword";
        sslToolkitCommandLine.parse("-R", "-S", testPassword);
        List<String> keyStorePasswords = sslToolkitCommandLine.getKeyStorePasswords();
        assertEquals(1, keyStorePasswords.size());
        assertEquals(testPassword, keyStorePasswords.get(0));
        assertEquals(keyStorePasswords, sslToolkitCommandLine.getKeyPasswords());
    }

    @Test
    public void testSameKeyAndKeystorePasswordWithKeyPasswordSpecified() {
        try {
            sslToolkitCommandLine.parse("-R", "-K", "testPassword");
            fail("Expected error when specifying same key and password with a key password");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_SAME_KEY_AND_KEY_PASSWORD, e.getExitCode());
        }
    }

    @Test
    public void testKeyStorePasswordArg() throws CommandLineParseException {
        String testPassword = "testPassword";
        sslToolkitCommandLine.parse("-S", testPassword);
        List<String> keyStorePasswords = sslToolkitCommandLine.getKeyStorePasswords();
        assertEquals(1, keyStorePasswords.size());
        assertEquals(testPassword, keyStorePasswords.get(0));
    }

    @Test
    public void testMultipleKeystorePasswordArgs() throws CommandLineParseException {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        sslToolkitCommandLine.parse("-n", "nifi1,nifi2", "-S", testPassword1, "-S", testPassword2);
        List<String> keyStorePasswords = sslToolkitCommandLine.getKeyStorePasswords();
        assertEquals(2, keyStorePasswords.size());
        assertEquals(testPassword1, keyStorePasswords.get(0));
        assertEquals(testPassword2, keyStorePasswords.get(1));
    }

    @Test
    public void testMultipleKeystorePasswordArgSingleHost() {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        try {
            sslToolkitCommandLine.parse("-S", testPassword1, "-S", testPassword2);
            fail("Expected error with mismatch keystore password number");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_INCORRECT_NUMBER_OF_PASSWORDS, e.getExitCode());
        }
    }

    @Test
    public void testKeyPasswordArg() throws CommandLineParseException {
        String testPassword = "testPassword";
        sslToolkitCommandLine.parse("-K", testPassword);
        List<String> keyPasswords = sslToolkitCommandLine.getKeyPasswords();
        assertEquals(1, keyPasswords.size());
        assertEquals(testPassword, keyPasswords.get(0));
    }

    @Test
    public void testMultipleKeyPasswordArgs() throws CommandLineParseException {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        sslToolkitCommandLine.parse("-n", "nifi1,nifi2", "-K", testPassword1, "-K", testPassword2);
        List<String> keyPasswords = sslToolkitCommandLine.getKeyPasswords();
        assertEquals(2, keyPasswords.size());
        assertEquals(testPassword1, keyPasswords.get(0));
        assertEquals(testPassword2, keyPasswords.get(1));
    }

    @Test
    public void testMultipleKeyPasswordArgSingleHost() {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        try {
            sslToolkitCommandLine.parse("-K", testPassword1, "-K", testPassword2);
            fail("Expected error with mismatch keystore password number");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_INCORRECT_NUMBER_OF_PASSWORDS, e.getExitCode());
        }
    }

    @Test
    public void testTruststorePasswordArg() throws CommandLineParseException {
        String testPassword = "testPassword";
        sslToolkitCommandLine.parse("-T", testPassword);
        List<String> trustStorePasswords = sslToolkitCommandLine.getTrustStorePasswords();
        assertEquals(1, trustStorePasswords.size());
        assertEquals(testPassword, trustStorePasswords.get(0));
    }

    @Test
    public void testMultipleTruststorePasswordArgs() throws CommandLineParseException {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        sslToolkitCommandLine.parse("-n", "nifi1,nifi2", "-T", testPassword1, "-T", testPassword2);
        List<String> trustStorePasswords = sslToolkitCommandLine.getTrustStorePasswords();
        assertEquals(2, trustStorePasswords.size());
        assertEquals(testPassword1, trustStorePasswords.get(0));
        assertEquals(testPassword2, trustStorePasswords.get(1));
    }

    @Test
    public void testMultipleTruststorePasswordArgSingleHost() {
        String testPassword1 = "testPassword1";
        String testPassword2 = "testPassword2";
        try {
            sslToolkitCommandLine.parse("-T", testPassword1, "-T", testPassword2);
            fail("Expected error with mismatch keystore password number");
        } catch (CommandLineParseException e) {
            assertEquals(SSLToolkitCommandLine.ERROR_INCORRECT_NUMBER_OF_PASSWORDS, e.getExitCode());
        }
    }

    private Properties getProperties() throws IOException {
        NiFiPropertiesWriter niFiPropertiesWriter = sslToolkitCommandLine.getNiFiPropertiesWriterFactory().create();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        niFiPropertiesWriter.writeNiFiProperties(byteArrayOutputStream);
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        return properties;
    }
}
