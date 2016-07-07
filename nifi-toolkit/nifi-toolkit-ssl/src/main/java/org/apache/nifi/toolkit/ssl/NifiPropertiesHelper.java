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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NifiPropertiesHelper {
    private final List<String> lines;

    public NifiPropertiesHelper() throws IOException {
        this(SSLToolkitMain.class.getClassLoader().getResourceAsStream("conf/nifi.properties"));
    }

    public NifiPropertiesHelper(InputStream inputStream) throws IOException {
        lines = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line.trim());
            }
        }
    }

    public void outputWithUpdatedPropertyValues(OutputStream outputStream, Map<String, String> updatedValues) throws IOException {
        Set<String> keysSeen = new HashSet<>();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            for (String line : lines) {
                String key = line.split("=")[0].trim();
                boolean outputLine = true;
                if (!key.isEmpty() && !key.startsWith("#")) {
                    if (!keysSeen.add(key)) {
                        throw new IOException("Found key more than once in nifi.properties: " + key);
                    }
                    String value = updatedValues.remove(key);
                    if (value != null) {
                        writer.write(key + "=" + value);
                        outputLine = false;
                    }
                }
                if (outputLine) {
                    writer.write(line);
                }
                writer.newLine();
            }
        }
    }
}
