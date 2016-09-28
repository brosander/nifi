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

package org.apache.nifi.toolkit.tls.keystore;

import org.apache.nifi.toolkit.tls.configuration.KeyStoreConfig;
import org.apache.nifi.toolkit.tls.util.InputStreamFactory;
import org.apache.nifi.toolkit.tls.util.OutputStreamFactory;
import org.apache.nifi.toolkit.tls.util.PasswordUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;

/**
 * Base class for managing KeyStores and Certificates
 */
public class KeyStoreManager {
    public static final String PKCS_12 = "PKCS12";

    private final Logger logger = LoggerFactory.getLogger(KeyStoreManager.class);

    private final KeyStoreConfig keyStoreConfig;
    private final KeyStore keyStore;
    private final OutputStreamFactory outputStreamFactory;
    private final File baseDir;

    public KeyStoreManager(KeyStoreConfig keyStoreConfig, File baseDir) throws GeneralSecurityException, IOException {
        this(keyStoreConfig, baseDir, new PasswordUtil(), FileInputStream::new, FileOutputStream::new);
    }

    public KeyStoreManager(KeyStoreConfig keyStoreConfig, File baseDir, PasswordUtil passwordUtil, InputStreamFactory inputStreamFactory, OutputStreamFactory outputStreamFactory) throws GeneralSecurityException, IOException {
        this.keyStoreConfig = keyStoreConfig;
        this.keyStore = loadKeystore(baseDir, inputStreamFactory);
        this.outputStreamFactory = outputStreamFactory;
        this.baseDir = baseDir;
    }

    /**
     * Returns the KeyStore
     *
     * @return the KeyStore
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * Returns the entry with a given alias
     *
     * @param alias
     * @return an entry from the keystore with the given alias
     * @throws GeneralSecurityException
     */
    public KeyStore.Entry getEntry(String alias) throws GeneralSecurityException {
        return getEntry(KeyStore.Entry.class, alias);
    }

    /**
     * Returns an entry from the KeyStore with the given alias
     *
     * @param clazz the expected type
     * @param alias the alias
     * @return an entry from the KeyStore with the given alias
     * @throws GeneralSecurityException if there is a problem retrieving the entry
     */
    public <T extends KeyStore.Entry> T getEntry(Class<T> clazz, String alias) throws GeneralSecurityException {
        String keyPassword = keyStoreConfig.getKeyPassword();
        KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPassword == null ? null : keyPassword.toCharArray()));
        if (entry == null) {
            return null;
        } else if (!clazz.isInstance(entry)) {
            throw new GeneralSecurityException("Expected " + alias + " entry to be of type " + clazz + " but was " + entry.getClass());
        }
        return (T) entry;
    }

    /**
     * Adds the private key of the KeyPair to the KeyStore and returns the entry
     *
     * @param keyPair      the KeyPair
     * @param alias        the alias
     * @param certificates the certificate chain
     * @return the entry
     * @throws GeneralSecurityException if there is a problem performing the operation
     */
    public KeyStore.PrivateKeyEntry addPrivateKeyToKeyStore(KeyPair keyPair, String alias, Certificate... certificates) throws GeneralSecurityException {
        String keyPassword = keyStoreConfig.getKeyPassword();
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPassword == null ? null : keyPassword.toCharArray(), certificates);
        return getEntry(KeyStore.PrivateKeyEntry.class, alias);
    }

    private KeyStore getInstance(String keyStoreType) throws KeyStoreException, NoSuchProviderException {
        if (PKCS_12.equalsIgnoreCase(keyStoreType)) {
            return KeyStore.getInstance(keyStoreType, BouncyCastleProvider.PROVIDER_NAME);
        } else {
            return KeyStore.getInstance(keyStoreType);
        }
    }

    protected KeyStore loadKeystore(File baseDir, InputStreamFactory inputStreamFactory) throws GeneralSecurityException, IOException {
        KeyStore result = getInstance(keyStoreConfig.getKeyStoreType());
        File keyStoreFile = new File(baseDir, keyStoreConfig.getKeyStore());
        if (keyStoreFile.exists()) {
            try (InputStream stream = inputStreamFactory.create(keyStoreFile)) {
                result.load(stream, keyStoreConfig.getKeyStorePassword().toCharArray());
            }
            return result;
        }
        result.load(null, null);
        return result;
    }

    public void storeKeystore(File file) throws GeneralSecurityException, IOException {
        String keyStorePassword = keyStoreConfig.getKeyStorePassword();
        try (OutputStream outputStream = outputStreamFactory.create(file)) {
            getKeyStore().store(outputStream, keyStorePassword.toCharArray());
        }
    }

    public void storeKeystore() throws GeneralSecurityException, IOException {
        storeKeystore(new File(baseDir, keyStoreConfig.getKeyStore()));
    }

    public KeyStoreConfig getKeyStoreConfig() {
        return keyStoreConfig;
    }
}
