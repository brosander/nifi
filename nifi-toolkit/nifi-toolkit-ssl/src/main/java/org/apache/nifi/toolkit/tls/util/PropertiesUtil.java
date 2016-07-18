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

package org.apache.nifi.toolkit.tls.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PropertiesUtil {
    public static Map<String, String> loadToMap(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } finally {
            inputStream.close();
        }
        return Collections.list(properties.propertyNames()).stream().map(String::valueOf).collect(Collectors.toMap(Function.identity(), properties::getProperty));
    }

    public static void saveFromMap(Map<String, String> map, OutputStream outputStream) throws IOException {
        Properties properties = new Properties(){
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(Collections.list(super.keys())));
            }
        };
        properties.putAll(map.entrySet().stream()
                .filter(stringStringEntry -> stringStringEntry.getKey() != null && stringStringEntry.getValue() != null)
                .collect(Collectors.toMap(stringStringEntry -> stringStringEntry.getKey(), stringStringEntry -> stringStringEntry.getValue())));
        try {
            properties.store(outputStream, null);
        } finally {
            outputStream.close();
        }
    }
}
