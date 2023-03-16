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
import static org.junit.Assert.fail;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.coprocessor.CoprocessorHost;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.AccessController;
import org.apache.hadoop.hbase.security.access.Permission.Action;
import org.apache.hadoop.hbase.security.token.TokenProvider;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.phoenix.jdbc.PhoenixDatabaseMetaData;
import org.apache.phoenix.queryserver.QueryServerOptions;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.client.Driver;
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
public class HttpParamImpersonationQueryServerIT {

    private static final Logger LOG = LoggerFactory.getLogger(HttpParamImpersonationQueryServerIT.class);
    private static QueryServerEnvironment environment;

    private static final List<TableName> SYSTEM_TABLE_NAMES = Arrays.asList(PhoenixDatabaseMetaData.SYSTEM_CATALOG_HBASE_TABLE_NAME,
        PhoenixDatabaseMetaData.SYSTEM_MUTEX_HBASE_TABLE_NAME,
        PhoenixDatabaseMetaData.SYSTEM_FUNCTION_HBASE_TABLE_NAME,
        PhoenixDatabaseMetaData.SYSTEM_SCHEMA_HBASE_TABLE_NAME,
        PhoenixDatabaseMetaData.SYSTEM_SEQUENCE_HBASE_TABLE_NAME,
        PhoenixDatabaseMetaData.SYSTEM_STATS_HBASE_TABLE_NAME);

    @Parameters(name = "tls = {0}")
    public static synchronized Iterable<Boolean> data() {
        return Arrays.asList(new Boolean[] {false, true});
    }

    public HttpParamImpersonationQueryServerIT(Boolean tls) throws Exception {
        //Clean up previous environment if any (Junit 4.13 @BeforeParam / @AfterParam would be an alternative)
        if(environment != null) {
            stopEnvironment();
        }

        final Configuration conf = new Configuration();
        conf.setStrings(CoprocessorHost.MASTER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
        conf.setStrings(CoprocessorHost.REGIONSERVER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
        conf.setStrings(CoprocessorHost.REGION_COPROCESSOR_CONF_KEY, AccessController.class.getName(), TokenProvider.class.getName());

        // user1 is allowed to impersonate others, user2 is not
        conf.set("hadoop.proxyuser.user1.groups", "*");
        conf.set("hadoop.proxyuser.user1.hosts", "*");
        conf.setBoolean(QueryServerProperties.QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR_ATTRIB, true);
        environment = new QueryServerEnvironment(conf, 2, tls);
    }

    @AfterClass
    public static synchronized void stopEnvironment() throws Exception {
        environment.stop();
    }

    private String getUrlTemplate() {
        String url = Driver.CONNECT_STRING_PREFIX + "url=%s://localhost:" + environment.getPqsPort() + "?"
                + QueryServerOptions.DEFAULT_QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM + "=%s;authentication=SPNEGO;serialization=PROTOBUF%s";
        if(environment.getTls()) {
            return String.format(url, "https", "%s", ";truststore=" +TlsUtil.getTrustStoreFile().getAbsolutePath() 
                + ";truststore_password="+TlsUtil.getTrustStorePassword());
        } else {
            return String.format(url, "http", "%s", "");
        }
    }

    @Test
    public void testSuccessfulImpersonation() throws Exception {
        final Entry<String,File> user1 = environment.getUser(1);
        final Entry<String,File> user2 = environment.getUser(2);
        // Build the JDBC URL by hand with the doAs
        final String doAsUrlTemplate = getUrlTemplate();
        final String tableName = "POSITIVE_IMPERSONATION";
        final int numRows = 5;
        final UserGroupInformation serviceUgi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(environment.getServicePrincipal() + "@" + environment.getRealm(), environment.getServiceKeytab().getAbsolutePath());
        serviceUgi.doAs(new PrivilegedExceptionAction<Void>() {
            @Override public Void run() throws Exception {
                createTable(tableName, numRows);
                grantUsersToPhoenixSystemTables(Arrays.asList(user1.getKey(), user2.getKey()));
                return null;
            }
        });
        UserGroupInformation user1Ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(user1.getKey() + "@" + environment.getRealm(), user1.getValue().getAbsolutePath());
        user1Ugi.doAs(new PrivilegedExceptionAction<Void>() {
            @Override public Void run() throws Exception {
                // This user should not be able to read the table
                readAndExpectPermissionError(environment.getPqsUrl(), tableName, numRows);
                // Run the same query with the same credentials, but with a doAs. We should be permitted since the user we're impersonating can run the query
                final String doAsUrl = String.format(doAsUrlTemplate, serviceUgi.getShortUserName());
                try (Connection conn = DriverManager.getConnection(doAsUrl);
                        Statement stmt = conn.createStatement()) {
                    conn.setAutoCommit(true);
                    readRows(stmt, tableName, numRows);
                }
                return null;
            }
        });
    }

    @Test
    public void testDisallowedImpersonation() throws Exception {
        final Entry<String,File> user2 = environment.getUser(2);
        // Build the JDBC URL by hand with the doAs
        final String doAsUrlTemplate = getUrlTemplate();
        final String tableName = "DISALLOWED_IMPERSONATION";
        final int numRows = 5;
        final UserGroupInformation serviceUgi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(environment.getServicePrincipal() + "@" + environment.getRealm(), environment.getServiceKeytab().getAbsolutePath());
        serviceUgi.doAs(new PrivilegedExceptionAction<Void>() {
            @Override public Void run() throws Exception {
                createTable(tableName, numRows);
                grantUsersToPhoenixSystemTables(Arrays.asList(user2.getKey()));
                return null;
            }
        });
        UserGroupInformation user2Ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(user2.getKey() + "@" + environment.getRealm(), user2.getValue().getAbsolutePath());
        user2Ugi.doAs(new PrivilegedExceptionAction<Void>() {
            @Override public Void run() throws Exception {
                // This user is disallowed to read this table
                readAndExpectPermissionError(environment.getPqsUrl(), tableName, numRows);
                // This user is also not allowed to impersonate
                final String doAsUrl = String.format(doAsUrlTemplate, serviceUgi.getShortUserName());
                try (Connection conn = DriverManager.getConnection(doAsUrl);
                        Statement stmt = conn.createStatement()) {
                    conn.setAutoCommit(true);
                    readRows(stmt, tableName, numRows);
                    fail("user2 should not be allowed to impersonate the service user");
                } catch (Exception e) {
                    LOG.info("Caught expected exception", e);
                }
                return null;
            }
        });
    }

    void createTable(String tableName, int numRows) throws Exception {
        try (Connection conn = DriverManager.getConnection(environment.getPqsUrl());
            Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(true);
            assertFalse(stmt.execute("CREATE TABLE " + tableName + "(pk integer not null primary key)"));
            for (int i = 0; i < numRows; i++) {
                assertEquals(1, stmt.executeUpdate("UPSERT INTO " + tableName + " values(" + i + ")"));
            }
            readRows(stmt, tableName, numRows);
        }
    }

    void grantUsersToPhoenixSystemTables(List<String> usersToGrant) throws Exception {
        // Grant permission to the user to access the system tables
        try {
            for (String user : usersToGrant) {
                for (TableName tn : SYSTEM_TABLE_NAMES) {
                    AccessControlClient.grant(environment.getUtil().getConnection(), tn, user, null, null, Action.READ, Action.EXEC);
                }
            }
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    void readAndExpectPermissionError(String jdbcUrl, String tableName, int numRows) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
            Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(true);
            readRows(stmt, tableName, numRows);
            fail("Expected an exception reading another user's table");
        } catch (Exception e) {
            LOG.debug("Caught expected exception", e);
            // Avatica doesn't re-create new exceptions across the wire. Need to just look at the contents of the message.
            String errorMessage = e.getMessage();
            assertTrue("Expected the error message to contain an HBase AccessDeniedException", errorMessage.contains("org.apache.hadoop.hbase.security.AccessDeniedException"));
            // Expecting an error message like: "Insufficient permissions for user 'user1' (table=POSITIVE_IMPERSONATION, action=READ)"
            // Being overly cautious to make sure we don't inadvertently pass the test due to permission errors on phoenix system tables.
            assertTrue("Expected message to contain " + tableName + " and READ", errorMessage.contains(tableName) && errorMessage.contains("READ"));
        }
    }

    void readRows(Statement stmt, String tableName, int numRows) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            for (int i = 0; i < numRows; i++) {
                assertTrue(rs.next());
                assertEquals(i, rs.getInt(1));
            }
            assertFalse(rs.next());
        }
    }
}
