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
package org.apache.phoenix.queryserver.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryServerTest {

  private static String getSpnegoPrincipal(String instance) {
    return "HTTP/" + instance + "@EXAMPLE.COM";
  }

  private static String EXPECTED_HOSTNAME;
  private QueryServer qs;
  private Configuration conf;

  @BeforeClass
  public static void setupOnce() throws IOException {
    EXPECTED_HOSTNAME = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
  }

  @Before
  public void setup() {
    this.conf = new Configuration(false);
    this.qs = new QueryServer();
  }

  @Test
  public void testHostExpansion() throws IOException {
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB, getSpnegoPrincipal("_HOST"));

    assertEquals(getSpnegoPrincipal(EXPECTED_HOSTNAME), qs.getSpnegoPrincipal(conf));
  }

  @Test
  public void testHostExpansionWithOldName() throws IOException {
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB_LEGACY, getSpnegoPrincipal("_HOST"));

    assertEquals(getSpnegoPrincipal(EXPECTED_HOSTNAME), qs.getSpnegoPrincipal(conf));
  }

  @Test
  public void testHostExpansionWithOldAndNewNames() throws IOException {
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB, getSpnegoPrincipal("_HOST"));
    // When we provide both names, the new property should take priority
    conf.set(QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB_LEGACY, "fake_" + getSpnegoPrincipal("_HOST"));

    assertEquals(getSpnegoPrincipal(EXPECTED_HOSTNAME), qs.getSpnegoPrincipal(conf));
  }

}
