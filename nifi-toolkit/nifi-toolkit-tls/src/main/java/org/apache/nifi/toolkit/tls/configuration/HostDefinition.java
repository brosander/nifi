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

import org.apache.nifi.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HostDefinition {
    private static final Pattern RANGE_PATTERN = Pattern.compile("^[0-9]+(-[0-9]+)?$");
    private final String hostname;
    private final int number;
    private final String keyStorePassword;
    private final String keyPassword;
    private final String trustStorePassword;

    public HostDefinition(String hostname, int number, String keyStorePassword, String keyPassword, String trustStorePassword) {
        this.hostname = hostname;
        this.number = number;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.trustStorePassword = trustStorePassword;
    }

    public static List<HostDefinition> createDefinitions(Stream<String> hostnameExpressions, Supplier<String> keyStorePasswords, Supplier<String> keyPasswords,
                                                         Supplier<String> trustStorePasswords) {
        return hostnameExpressions.flatMap(hostnameExpression -> extractHostnames(hostnameExpression).flatMap(hostname -> {
            ExtractedRange extractedRange = new ExtractedRange(hostname, '(', ')');
            if (extractedRange.range == null) {
                return Stream.of(createDefinition(hostname, 1, keyStorePasswords, keyPasswords, trustStorePasswords));
            }
            if (!StringUtils.isEmpty(extractedRange.afterClose)) {
                throw new IllegalArgumentException("No characters expected after )");
            }
            return extractedRange.range.map(numString -> createDefinition(extractedRange.beforeOpen, Integer.parseInt(numString), keyStorePasswords, keyPasswords, trustStorePasswords));
        })).collect(Collectors.toList());
    }

    protected static HostDefinition createDefinition(String hostname, int number, Supplier<String> keyStorePasswords, Supplier<String> keyPasswords, Supplier<String> trustStorePasswords) {
        String keyStorePassword = keyStorePasswords.get();
        String keyPassword;
        if (keyPasswords == null) {
            keyPassword = keyStorePassword;
        } else {
            keyPassword = keyPasswords.get();
        }
        String trustStorePassword = trustStorePasswords.get();
        return new HostDefinition(hostname, number, keyStorePassword, keyPassword, trustStorePassword);
    }

    protected static Stream<String> extractHostnames(String hostname) {
        ExtractedRange extractedRange = new ExtractedRange(hostname, '[', ']');
        if (extractedRange.range == null) {
            return Stream.of(hostname);
        }
        return extractedRange.range.map(s -> extractedRange.beforeOpen + s + extractedRange.afterClose).flatMap(HostDefinition::extractHostnames);
    }

    private static Stream<String> extractRange(String range) {
        if (!RANGE_PATTERN.matcher(range).matches()) {
            throw new IllegalArgumentException("Expected either one number or two separated by a single hyphen");
        }
        String[] split = range.split("-");
        if (split.length == 1) {
            String prefix = "1-";
            if (split[0].charAt(0) == '0') {
                prefix = String.format("%0" + split[0].length() + "d-", 1);
            }
            return extractRange(prefix + split[0]);
        } else {
            int baseLength = split[0].length();
            int low = Integer.parseInt(split[0]);
            String padding = split[0].substring(0, split[0].length() - Integer.toString(low).length());
            int high = Integer.parseInt(split[1]);
            return IntStream.range(low, high + 1).mapToObj(i -> {
                String s = Integer.toString(i);
                int length = s.length();
                if (length >= baseLength) {
                    return s;
                } else {
                    return padding.substring(0, baseLength - length) + s;
                }
            });
        }
    }

    public String getHostname() {
        return hostname;
    }

    public int getNumber() {
        return number;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    private static class ExtractedRange {
        private final String beforeOpen;
        private final Stream<String> range;
        private final String afterClose;

        public ExtractedRange(String string, char rangeOpen, char rangeClose) {
            int openBracket = string.indexOf(rangeOpen);
            if (openBracket >= 0) {
                int closeBracket = string.indexOf(rangeClose, openBracket);
                if (closeBracket < 0) {
                    throw new IllegalArgumentException("Unable to find matching " + rangeClose + " for " + rangeOpen + " in " + string);
                }
                beforeOpen = string.substring(0, openBracket);
                if (closeBracket + 1 < string.length()) {
                    afterClose = string.substring(closeBracket + 1);
                } else {
                    afterClose = "";
                }
                range = extractRange(string.substring(openBracket + 1, closeBracket));
            } else {
                beforeOpen = string;
                range = null;
                afterClose = "";
            }
        }
    }
}
