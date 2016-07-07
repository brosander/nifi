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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class SSLHelperTest {
    SSLHelper sslHelper;

    private int days;

    private int keySize;

    private String keyPairAlgorithm;

    private String signingAlgorithm;

    private String keyStoreType;

    private SecureRandom secureRandom;

    private KeyPairGenerator keyPairGenerator;

    @BeforeClass
    public static void beforeClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() throws NoSuchAlgorithmException {
        days = 360;
        keySize = 2048;
        keyPairAlgorithm = "RSA";
        signingAlgorithm = "SHA1WITHRSA";
        keyStoreType = KeyStore.getDefaultType();
        secureRandom = mock(SecureRandom.class);
        keyPairGenerator = KeyPairGenerator.getInstance(keyPairAlgorithm);
        keyPairGenerator.initialize(keySize);
        sslHelper = new SSLHelper(secureRandom, keyPairGenerator, days, signingAlgorithm, keyStoreType);
    }

    private Date inFuture(int days) {
        return new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days));
    }

    @Test
    public void testGenerateSelfSignedCert() throws GeneralSecurityException, IOException, OperatorCreationException {
        String dn = "CN=testDN,O=testOrg";

        X509Certificate x509Certificate = sslHelper.generateSelfSignedX509Certificate(sslHelper.generateKeyPair(), dn);

        Date notAfter = x509Certificate.getNotAfter();
        assertTrue(notAfter.after(inFuture(days - 1)));
        assertTrue(notAfter.before(inFuture(days + 1)));

        Date notBefore = x509Certificate.getNotBefore();
        assertTrue(notBefore.after(inFuture(-1)));
        assertTrue(notBefore.before(inFuture(1)));

        assertEquals(dn, x509Certificate.getIssuerDN().getName());
        assertEquals(signingAlgorithm, x509Certificate.getSigAlgName());
        assertEquals(keyPairAlgorithm, x509Certificate.getPublicKey().getAlgorithm());

        x509Certificate.checkValidity();
    }

    @Test
    public void testGeneratePassword() {
        int value = 8675309;
        doAnswer(invocation -> {
            byte[] bytes = (byte[]) invocation.getArguments()[0];
            assertEquals(17, bytes.length);
            Arrays.fill(bytes, (byte) 0);
            byte[] val = ByteBuffer.allocate(Long.BYTES).putLong(value).array();
            System.arraycopy(val, 0, bytes, bytes.length - val.length, val.length);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));
        String expected = BigInteger.valueOf(Integer.valueOf(value).longValue()).toString(32);
        String actual = sslHelper.generatePassword();
        assertEquals(expected, actual);
    }
}
