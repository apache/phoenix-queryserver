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

import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class AdminCommandRequestTest {

    @Test
    public void testDeserialization() throws Exception {
        String json = "{\"command\":\"IndexTool\",\"parameters\":{\"param1\":\"value1\"}}";
        AdminCommandRequest adminRequest = new ObjectMapper().readValue(json, AdminCommandRequest.class);
        assertEquals(adminRequest.getCommand(), AdminCommand.IndexTool);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        assertEquals(adminRequest.getParameters(), parameters);
    }
}
