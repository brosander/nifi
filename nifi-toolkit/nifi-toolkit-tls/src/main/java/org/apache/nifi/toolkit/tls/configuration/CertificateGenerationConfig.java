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

package org.apache.nifi.toolkit.tls.configuration;

import org.apache.nifi.security.util.CertificateUtils;
import org.apache.nifi.util.StringUtils;

public class CertificateGenerationConfig extends BaseConfiguration {
    public static final int DEFAULT_KEY_SIZE = 2048;
    public static final String DEFAULT_KEY_PAIR_ALGORITHM = "RSA";
    public static final String DEFAULT_SIGNING_ALGORITHM = "SHA256WITHRSA";
    public static final String DEFAULT_DN_PREFIX = "CN=";
    public static final String DEFAULT_DN_SUFFIX = ",OU=NIFI";

    private String dn;
    private int keySize;
    private String keyPairAlgorithm;
    private String signingAlgorithm;
    private String dnPrefix;
    private String dnSuffix;

    public CertificateGenerationConfig() {
    }

    public CertificateGenerationConfig(CertificateGenerationConfig certificateGenerationConfig) {
        super(certificateGenerationConfig);
        setDn(certificateGenerationConfig.getDn());
        setKeySize(certificateGenerationConfig.getKeySize());
        setKeyPairAlgorithm(certificateGenerationConfig.getKeyPairAlgorithm());
        setSigningAlgorithm(certificateGenerationConfig.getSigningAlgorithm());
        setDnPrefix(certificateGenerationConfig.getDnPrefix());
        setDnSuffix(certificateGenerationConfig.getDnSuffix());
    }

    public String calcDefaultDn(String hostname) {
        return CertificateUtils.reorderDn(dnPrefix + hostname + dnSuffix);
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public String getKeyPairAlgorithm() {
        return keyPairAlgorithm;
    }

    public void setKeyPairAlgorithm(String keyPairAlgorithm) {
        this.keyPairAlgorithm = keyPairAlgorithm;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDnPrefix() {
        return dnPrefix;
    }

    public void setDnPrefix(String dnPrefix) {
        this.dnPrefix = dnPrefix;
    }

    public String getDnSuffix() {
        return dnSuffix;
    }

    public void setDnSuffix(String dnSuffix) {
        this.dnSuffix = dnSuffix;
    }

    @Override
    public void initDefaults() {
        if (keySize == 0) {
            keySize = DEFAULT_KEY_SIZE;
        }
        if (StringUtils.isEmpty(keyPairAlgorithm)) {
            keyPairAlgorithm = DEFAULT_KEY_PAIR_ALGORITHM;
        }
        if (StringUtils.isEmpty(signingAlgorithm)) {
            signingAlgorithm = DEFAULT_SIGNING_ALGORITHM;
        }
        if (StringUtils.isEmpty(dn)) {
            dn = calcDefaultDn("localhost");
        }
        if (StringUtils.isEmpty(dnPrefix)) {
            dnPrefix = DEFAULT_DN_PREFIX;
        }
        if (StringUtils.isEmpty(dnSuffix)) {
            dnSuffix = DEFAULT_DN_SUFFIX;
        }
        super.initDefaults();
    }
}
