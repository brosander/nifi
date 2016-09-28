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

package org.apache.nifi.toolkit.tls.commandLine.base;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.nifi.toolkit.tls.commandLine.CommandLineParseException;
import org.apache.nifi.toolkit.tls.commandLine.ExitCode;
import org.apache.nifi.toolkit.tls.commandLine.OptionWithDefault;
import org.apache.nifi.toolkit.tls.configuration.TlsConfig;
import org.apache.nifi.util.StringUtils;

import java.io.File;

/**
 * Common base argument logic for the CA server and client
 */
public abstract class BaseCertificateAuthorityCommandLine extends BaseCommandLine {
    public static final String TOKEN_ARG = "token";
    public static final String CONFIG_JSON_ARG = "configJson";
    public static final String USE_CONFIG_JSON_ARG = "useConfigJson";
    public static final String PORT_ARG = "PORT";

    public static final String DEFAULT_CONFIG_JSON = new File("config.json").getPath();

    private static final OptionWithDefault CONFIG_JSON_OPTION = new OptionWithDefault("f", CONFIG_JSON_ARG, "The place to write configuration info", DEFAULT_CONFIG_JSON);
    private static final Option USE_CONFIG_JSON_OPTION = new Option("F", USE_CONFIG_JSON_ARG, false,
            "Flag specifying that all configuration is read from " + CONFIG_JSON_ARG + " to facilitate automated use (otherwise " + CONFIG_JSON_ARG + " will only be written to.");

    private final OptionWithDefault DN_OPTION = new OptionWithDefault("D", DN_ARG, getDnDescription(), TlsConfig.calcDefaultDn(getDnHostname()));
    private final OptionWithDefault PORT_OPTION = new OptionWithDefault("p", PORT_ARG, getPortDescription(), TlsConfig.DEFAULT_PORT);
    private final Option TOKEN_OPTION = new Option("t", TOKEN_ARG, true, getTokenDescription());


    private String token;
    private String configJson;
    private boolean onlyUseConfigJson;
    private int port;
    private String dn;

    public BaseCertificateAuthorityCommandLine(String header) {
        super(header);
        addOption(TOKEN_OPTION);
        addOption(CONFIG_JSON_OPTION.getOption());
        addOption(USE_CONFIG_JSON_OPTION);
        addOption(PORT_OPTION.getOption());
        addOption(DN_OPTION.getOption());
    }

    protected abstract String getTokenDescription();

    protected abstract String getDnDescription();

    protected abstract String getPortDescription();

    protected abstract String getDnHostname();

    @Override
    protected CommandLine doParse(String[] args) throws CommandLineParseException {
        CommandLine commandLine = super.doParse(args);

        token = commandLine.getOptionValue(TOKEN_ARG);
        onlyUseConfigJson = commandLine.hasOption(USE_CONFIG_JSON_ARG);
        if (StringUtils.isEmpty(token) && !onlyUseConfigJson) {
            printUsageAndThrow(TOKEN_ARG + " argument must not be empty unless " + USE_CONFIG_JSON_ARG + " set", ExitCode.ERROR_TOKEN_ARG_EMPTY);
        }
        configJson = CONFIG_JSON_OPTION.getValue(commandLine);
        try {
            port = PORT_OPTION.getIntValue(commandLine);
        } catch (ParseException e) {
            return printUsageAndThrow("Error parsing command line. (" + e.getMessage() + ")", ExitCode.ERROR_PARSING_COMMAND_LINE);
        }
        dn = DN_OPTION.getValue(commandLine);
        return commandLine;
    }

    public String getToken() {
        return token;
    }

    public String getConfigJson() {
        return configJson;
    }

    public boolean onlyUseConfigJson() {
        return onlyUseConfigJson;
    }

    public int getPort() {
        return port;
    }

    public String getDn() {
        return dn;
    }
}
