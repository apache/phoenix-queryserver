/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.queryserver.server.admin;

import java.util.Map;

import org.apache.htrace.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class AdminCommandRequest {
    @JsonProperty(required = true)
    private AdminCommand command;
    private Map<String, String> parameters;

    public AdminCommandRequest() {

    }

    public AdminCommandRequest(AdminCommand command, Map<String, String> parameters) {
        this.command = command;
        this.parameters = parameters;
    }

    public AdminCommand getCommand() {
        return command;
    }

    public void setCommand(AdminCommand command) {
        this.command = command;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String[] getParametersAsInputArgs() {
        String[] args = parameters != null ? new String[parameters.size()] : new String[0];
        if (parameters != null) {
            int index = 0;
            for (String key : parameters.keySet()) {
                args[index++] = "-" + key + " " + parameters.get(key);
            }
        }
        return args;
    }
}
