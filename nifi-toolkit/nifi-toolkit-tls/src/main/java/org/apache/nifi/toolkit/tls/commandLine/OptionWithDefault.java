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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class OptionWithDefault {
    private final Option option;
    private final String defaultValue;

    public OptionWithDefault(String opt, String longOpt, String description, Object defaultValue) {
        String fullDescription = description;
        if (defaultValue != null) {
            fullDescription += " (default: " + defaultValue + ")";
        }
        this.option = new Option(opt, longOpt, true, fullDescription);
        this.defaultValue = String.valueOf(defaultValue);
    }

    public Option getOption() {
        return option;
    }

    public String getValue(CommandLine commandLine) {
        return commandLine.getOptionValue(option.getLongOpt(), defaultValue);
    }

    public int getIntValue(CommandLine commandLine) throws ParseException {
        try {
            return Integer.parseInt(getValue(commandLine));
        } catch (NumberFormatException e) {
            throw new ParseException("Expected integer for " + option + " argument. (" + e.getMessage() + ")");
        }
    }
}
