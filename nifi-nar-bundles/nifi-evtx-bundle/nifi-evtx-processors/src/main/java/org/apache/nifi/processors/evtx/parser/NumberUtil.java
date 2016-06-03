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

package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;

import java.io.IOException;

/**
 * Created by brosander on 6/1/16.
 */
public class NumberUtil {
    public static final String EXPECTED_TEXT = " Expected %s got %s.";

    public static int intValueExpected(Number number, int expected, String errorMessage, Object... args) throws IOException {
        int result = number.intValue();
        if (result != expected) {
            throw createException(errorMessage, args, expected, result);
        }
        return result;
    }

    public static int intValueMax(UnsignedInteger unsignedInteger, int max, String errorMessage, Object... args) throws IOException {
        if (unsignedInteger.compareTo(UnsignedInteger.valueOf(max)) > 0) {
            throw createException(errorMessage, args, "< " + max, unsignedInteger);
        }
        return unsignedInteger.intValue();
    }

    private static IOException createException(String errorMessage, Object[] args, Object expected, Object actual) {
        return new IOException(String.format(errorMessage, args) + String.format(EXPECTED_TEXT, expected, actual));
    }
}
