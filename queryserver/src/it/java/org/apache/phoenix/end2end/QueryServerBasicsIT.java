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
package org.apache.phoenix.end2end;

import static java.lang.String.format;
import static org.apache.phoenix.jdbc.PhoenixDatabaseMetaData.TABLE_CAT;
import static org.apache.phoenix.jdbc.PhoenixDatabaseMetaData.TABLE_CATALOG;
import static org.apache.phoenix.jdbc.PhoenixDatabaseMetaData.TABLE_SCHEM;
import static org.apache.phoenix.query.QueryConstants.SYSTEM_SCHEMA_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.query.QueryServices;
import org.apache.phoenix.queryserver.client.ThinClientUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Smoke test for query server.
 */
public class QueryServerBasicsIT extends BaseHBaseManagedTimeIT {

  private static final Log LOG = LogFactory.getLog(QueryServerBasicsIT.class);

  private static QueryServerThread AVATICA_SERVER;
  private static Configuration CONF;
  private static String CONN_STRING;

  @Rule
  public TestName name = new TestName();

  @BeforeClass
  public static void beforeClass() throws Exception {
    CONF = getTestClusterConfig();
    CONF.setInt(QueryServices.QUERY_SERVER_HTTP_PORT_ATTRIB, 0);
    String url = getUrl();
    AVATICA_SERVER = new QueryServerThread(new String[] { url }, CONF,
            QueryServerBasicsIT.class.getName());
    AVATICA_SERVER.start();
    AVATICA_SERVER.getQueryServer().awaitRunning();
    final int port = AVATICA_SERVER.getQueryServer().getPort();
    LOG.info("Avatica server started on port " + port);
    CONN_STRING = ThinClientUtil.getConnectionUrl("localhost", port);
    LOG.info("JDBC connection string is " + CONN_STRING);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    if (AVATICA_SERVER != null) {
      AVATICA_SERVER.join(TimeUnit.MINUTES.toMillis(1));
      Throwable t = AVATICA_SERVER.getQueryServer().getThrowable();
      if (t != null) {
        fail("query server threw. " + t.getMessage());
      }
      assertEquals("query server didn't exit cleanly", 0, AVATICA_SERVER.getQueryServer()
        .getRetCode());
    }
  }

  @Test
  public void testCatalogs() throws Exception {
    try (final Connection connection = DriverManager.getConnection(CONN_STRING)) {
      assertThat(connection.isClosed(), is(false));
      try (final ResultSet resultSet = connection.getMetaData().getCatalogs()) {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        assertFalse("unexpected populated resultSet", resultSet.next());
        assertEquals(1, metaData.getColumnCount());
        assertEquals(TABLE_CAT, metaData.getColumnName(1));
      }
    }
  }

  @Test
  public void testSchemas() throws Exception {
      Properties props=new Properties();
      props.setProperty(QueryServices.IS_NAMESPACE_MAPPING_ENABLED, Boolean.toString(true));
      try (final Connection connection = DriverManager.getConnection(CONN_STRING, props)) {
      connection.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS " + SYSTEM_SCHEMA_NAME);
      assertThat(connection.isClosed(), is(false));
      try (final ResultSet resultSet = connection.getMetaData().getSchemas()) {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        assertTrue("unexpected empty resultset", resultSet.next());
        assertEquals(2, metaData.getColumnCount());
        assertEquals(TABLE_SCHEM, metaData.getColumnName(1));
        assertEquals(TABLE_CATALOG, metaData.getColumnName(2));
        boolean containsSystem = false;
        do {
          if (resultSet.getString(1).equalsIgnoreCase(SYSTEM_SCHEMA_NAME)) containsSystem = true;
        } while (resultSet.next());
        assertTrue(format("should contain at least %s schema.", SYSTEM_SCHEMA_NAME), containsSystem);
      }
    }
  }

  @Test
  public void smokeTest() throws Exception {
    final String tableName = name.getMethodName();
    try (final Connection connection = DriverManager.getConnection(CONN_STRING)) {
      assertThat(connection.isClosed(), is(false));
      connection.setAutoCommit(true);
      try (final Statement stmt = connection.createStatement()) {
        assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
        assertFalse(stmt.execute("CREATE TABLE " + tableName + "("
            + "id INTEGER NOT NULL, "
            + "pk varchar(3) NOT NULL "
            + "CONSTRAINT PK_CONSTRAINT PRIMARY KEY (id, pk))"));
        assertEquals(0, stmt.getUpdateCount());
        assertEquals(1, stmt.executeUpdate("UPSERT INTO " + tableName + " VALUES(1, 'foo')"));
        assertEquals(1, stmt.executeUpdate("UPSERT INTO " + tableName + " VALUES(2, 'bar')"));
        assertTrue(stmt.execute("SELECT * FROM " + tableName));
        try (final ResultSet resultSet = stmt.getResultSet()) {
          assertTrue(resultSet.next());
          assertEquals(1, resultSet.getInt(1));
          assertEquals("foo", resultSet.getString(2));
          assertTrue(resultSet.next());
          assertEquals(2, resultSet.getInt(1));
          assertEquals("bar", resultSet.getString(2));
        }
      }
      final String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
      try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setInt(1, 1);
        try (ResultSet resultSet = stmt.executeQuery()) {
          assertTrue(resultSet.next());
          assertEquals(1, resultSet.getInt(1));
          assertEquals("foo", resultSet.getString(2));
        }
        stmt.clearParameters();
        stmt.setInt(1, 5);
        try (final ResultSet resultSet = stmt.executeQuery()) {
          assertFalse(resultSet.next());
        }
      }
    }
  }

  @Test
  public void arrayTest() throws Exception {
      final String tableName = name.getMethodName();
      try (Connection conn = DriverManager.getConnection(CONN_STRING);
              Statement stmt = conn.createStatement()) {
          conn.setAutoCommit(false);
          assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
          assertFalse(stmt.execute("CREATE TABLE " + tableName + " ("
              + "pk VARCHAR NOT NULL PRIMARY KEY, "
              + "histogram INTEGER[])")
              );
          conn.commit();
          int numRows = 10;
          int numEvenElements = 4;
          int numOddElements = 6;
          for (int i = 0; i < numRows; i++) {
              int arrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
              StringBuilder sb = new StringBuilder();
              for (int arrayOffset = 0; arrayOffset < arrayLength; arrayOffset++) {
                  if (sb.length() > 0) {
                      sb.append(", ");
                  }
                  sb.append(getArrayValueForOffset(arrayOffset));
              }
              String updateSql = "UPSERT INTO " + tableName + " values('" + i + "', " + "ARRAY[" + sb.toString() + "])";
              assertEquals(1, stmt.executeUpdate(updateSql));
          }
          conn.commit();
          try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
              for (int i = 0; i < numRows; i++) {
                  assertTrue(rs.next());
                  assertEquals(i, Integer.parseInt(rs.getString(1)));
                  Array array = rs.getArray(2);
                  Object untypedArrayData = array.getArray();
                  assertTrue("Expected array data to be an int array, but was " + untypedArrayData.getClass(), untypedArrayData instanceof Object[]);
                  Object[] arrayData = (Object[]) untypedArrayData;
                  int expectedArrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
                  assertEquals(expectedArrayLength, arrayData.length);
                  for (int arrayOffset = 0; arrayOffset < expectedArrayLength; arrayOffset++) {
                      assertEquals(getArrayValueForOffset(arrayOffset), arrayData[arrayOffset]);
                  }
              }
              assertFalse(rs.next());
          }
      }
  }

  @Test
  public void preparedStatementArrayTest() throws Exception {
      final String tableName = name.getMethodName();
      try (Connection conn = DriverManager.getConnection(CONN_STRING);
              Statement stmt = conn.createStatement()) {
          conn.setAutoCommit(false);
          assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
          assertFalse(stmt.execute("CREATE TABLE " + tableName + " ("
              + "pk VARCHAR NOT NULL PRIMARY KEY, "
              + "histogram INTEGER[])")
              );
          conn.commit();
          int numRows = 10;
          int numEvenElements = 4;
          int numOddElements = 6;
          try (PreparedStatement pstmt = conn.prepareStatement("UPSERT INTO " + tableName + " values(?, ?)")) {
              for (int i = 0; i < numRows; i++) {
                pstmt.setString(1, Integer.toString(i));
                int arrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
                Object[] arrayData = new Object[arrayLength];
                for (int arrayOffset = 0; arrayOffset < arrayLength; arrayOffset++) {
                  arrayData[arrayOffset] = getArrayValueForOffset(arrayOffset);
                }
                pstmt.setArray(2, conn.createArrayOf("INTEGER", arrayData));
                assertEquals(1, pstmt.executeUpdate());
              }
              conn.commit();
          }
          conn.commit();
          try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
              for (int i = 0; i < numRows; i++) {
                  assertTrue(rs.next());
                  assertEquals(i, Integer.parseInt(rs.getString(1)));
                  Array array = rs.getArray(2);
                  Object untypedArrayData = array.getArray();
                  assertTrue("Expected array data to be an int array, but was " + untypedArrayData.getClass(), untypedArrayData instanceof Object[]);
                  Object[] arrayData = (Object[]) untypedArrayData;
                  int expectedArrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
                  assertEquals(expectedArrayLength, arrayData.length);
                  for (int arrayOffset = 0; arrayOffset < expectedArrayLength; arrayOffset++) {
                      assertEquals(getArrayValueForOffset(arrayOffset), arrayData[arrayOffset]);
                  }
              }
              assertFalse(rs.next());
          }
      }
  }

  @Test
  public void preparedStatementVarcharArrayTest() throws Exception {
      final String tableName = name.getMethodName();
      try (Connection conn = DriverManager.getConnection(CONN_STRING);
              Statement stmt = conn.createStatement()) {
          conn.setAutoCommit(false);
          assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
          assertFalse(stmt.execute("CREATE TABLE " + tableName + " ("
              + "pk VARCHAR NOT NULL PRIMARY KEY, "
              + "histogram VARCHAR[])")
              );
          conn.commit();
          int numRows = 10;
          int numEvenElements = 4;
          int numOddElements = 6;
          try (PreparedStatement pstmt = conn.prepareStatement("UPSERT INTO " + tableName + " values(?, ?)")) {
              for (int i = 0; i < numRows; i++) {
                pstmt.setString(1, Integer.toString(i));
                int arrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
                Object[] arrayData = new Object[arrayLength];
                for (int arrayOffset = 0; arrayOffset < arrayLength; arrayOffset++) {
                  arrayData[arrayOffset] = Integer.toString(getArrayValueForOffset(arrayOffset));
                }
                pstmt.setArray(2, conn.createArrayOf("VARCHAR", arrayData));
                assertEquals(1, pstmt.executeUpdate());
              }
              conn.commit();
          }
          conn.commit();
          try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
              for (int i = 0; i < numRows; i++) {
                  assertTrue(rs.next());
                  assertEquals(i, Integer.parseInt(rs.getString(1)));
                  Array array = rs.getArray(2);
                  Object untypedArrayData = array.getArray();
                  assertTrue("Expected array data to be an int array, but was " + untypedArrayData.getClass(), untypedArrayData instanceof Object[]);
                  Object[] arrayData = (Object[]) untypedArrayData;
                  int expectedArrayLength = i % 2 == 0 ? numEvenElements : numOddElements;
                  assertEquals(expectedArrayLength, arrayData.length);
                  for (int arrayOffset = 0; arrayOffset < expectedArrayLength; arrayOffset++) {
                      assertEquals(Integer.toString(getArrayValueForOffset(arrayOffset)), arrayData[arrayOffset]);
                  }
              }
              assertFalse(rs.next());
          }
      }
  }

  private int getArrayValueForOffset(int arrayOffset) {
      return arrayOffset * 2 + 1;
  }

  @Test
  public void testParameterizedLikeExpression() throws Exception {
    final Connection conn = DriverManager.getConnection(CONN_STRING);
    final String tableName = generateUniqueName();
    conn.createStatement().execute(
            "CREATE TABLE " + tableName + " (k VARCHAR NOT NULL PRIMARY KEY, i INTEGER)");
    conn.commit();

    final PreparedStatement upsert = conn.prepareStatement(
            "UPSERT INTO " + tableName + " VALUES (?, ?)");
    upsert.setString(1, "123n7-app-2-");
    upsert.setInt(2, 1);
    upsert.executeUpdate();
    conn.commit();

    final PreparedStatement select = conn.prepareStatement(
            "select k from " + tableName + " where k like ?");
    select.setString(1, "12%");
    ResultSet rs = select.executeQuery();
    assertTrue(rs.next());
    assertEquals("123n7-app-2-", rs.getString(1));
    assertFalse(rs.next());

    select.setString(1, null);
    rs = select.executeQuery();
    assertFalse(rs.next());
  }
}
