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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.coprocessor.CoprocessorHost;
import org.apache.hadoop.hbase.security.token.TokenProvider;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
@Category(NeedsOwnMiniClusterTest.class)
public class SecureQueryServerIT {
    private static final Logger LOG = LoggerFactory.getLogger(SecureQueryServerIT.class);
    private static QueryServerEnvironment environment;

    @Parameters(name = "tls = {0}")
    public static synchronized Iterable<Boolean> data() {
        return Arrays.asList(new Boolean[] {false, true});
    }

    public SecureQueryServerIT(Boolean tls) throws Exception {
        //Clean up previous environment if any (Junit 4.13 @BeforeParam / @AfterParam would be an alternative)
        if(environment != null) {
            stopEnvironment();
        }

        final Configuration conf = new Configuration();
        conf.setStrings(CoprocessorHost.REGION_COPROCESSOR_CONF_KEY,
                    TokenProvider.class.getName());
        environment = new QueryServerEnvironment(conf, 3, tls);
    }


    @AfterClass
    public static synchronized void stopEnvironment() throws Exception {
        environment.stop();
    }

    @Test
    public void testBasicReadWrite() throws Exception {
        final Entry<String,File> user1 = environment.getUser(1);
        UserGroupInformation user1Ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(user1.getKey(), user1.getValue().getAbsolutePath());
        user1Ugi.doAs(new PrivilegedExceptionAction<Void>() {
            @Override public Void run() throws Exception {
                // Phoenix
                final String tableName = "phx_table1";
                try (java.sql.Connection conn = DriverManager.getConnection(environment.getPqsUrl());
                        Statement stmt = conn.createStatement()) {
                    conn.setAutoCommit(true);
                    assertFalse(stmt.execute("CREATE TABLE " + tableName + "(pk integer not null primary key)"));
                    final int numRows = 5;
                    for (int i = 0; i < numRows; i++) {
                      assertEquals(1, stmt.executeUpdate("UPSERT INTO " + tableName + " values(" + i + ")"));
                    }

                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
                        for (int i = 0; i < numRows; i++) {
                            assertTrue(rs.next());
                            assertEquals(i, rs.getInt(1));
                        }
                        assertFalse(rs.next());
                    }
                }
                return null;
            }
        });
    }
}
