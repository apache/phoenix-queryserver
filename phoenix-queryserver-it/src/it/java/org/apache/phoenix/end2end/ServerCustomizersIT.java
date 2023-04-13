/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.end2end;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.query.BaseTest;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.server.ServerCustomizersFactory;
import org.apache.phoenix.queryserver.server.customizers.BasicAuthenticationServerCustomizer;
import org.apache.phoenix.queryserver.server.customizers.BasicAuthenticationServerCustomizer.BasicAuthServerCustomizerFactory;
import org.apache.phoenix.util.InstanceResolver;
import org.apache.phoenix.util.ReadOnlyProps;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCustomizersIT extends BaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServerCustomizersIT.class);
    private static final String USER_NOT_AUTHORIZED = "user1";

    private static QueryServerTestUtil PQS_UTIL;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @BeforeClass
    public static synchronized void setup() throws Exception {
        setUpTestDriver(ReadOnlyProps.EMPTY_PROPS);

        Configuration conf = config;
        if(System.getProperty("do.not.randomize.pqs.port") == null) {
            conf.setInt(QueryServerProperties.QUERY_SERVER_HTTP_PORT_ATTRIB, 0);
        }
        PQS_UTIL = new QueryServerTestUtil(conf);
        PQS_UTIL.startLocalHBaseCluster(ServerCustomizersIT.class);
        // Register a test jetty server customizer
        InstanceResolver.clearSingletons();
        InstanceResolver.getSingleton(ServerCustomizersFactory.class, new BasicAuthServerCustomizerFactory());
        PQS_UTIL.startQueryServer();
    }

    @AfterClass
    public static synchronized void teardown() throws Exception {
        // Remove custom singletons for future tests
        InstanceResolver.clearSingletons();
        if (PQS_UTIL != null) {
            PQS_UTIL.stopQueryServer();
            PQS_UTIL.stopLocalHBaseCluster();
        }
    }

    @Test
    public void testUserAuthorized() throws Exception {
        try (Connection conn = DriverManager.getConnection(PQS_UTIL.getUrl(
                getBasicAuthParams(BasicAuthenticationServerCustomizer.USER_AUTHORIZED)));
                Statement stmt = conn.createStatement()) {
            Assert.assertFalse("user3 should have access", stmt.execute(
                "create table "+ServerCustomizersIT.class.getSimpleName()+" (pk integer not null primary key)"));
        }
    }

    @Test
    public void testUserNotAuthorized() throws Exception {
        expected.expect(RuntimeException.class);
        expected.expectMessage("HTTP/401");
        try (Connection conn = DriverManager.getConnection(PQS_UTIL.getUrl(
                getBasicAuthParams(USER_NOT_AUTHORIZED)));
                Statement stmt = conn.createStatement()) {
            Assert.assertFalse(stmt.execute(
                    "select access from database"));
        }
    }

    private Map<String, String> getBasicAuthParams(String user) {
        Map<String, String> params = new HashMap<>();
        params.put("authentication", "BASIC");
        params.put("avatica_user", user);
        params.put("avatica_password", BasicAuthenticationServerCustomizer.USER_PW);
        return params;
    }
}
