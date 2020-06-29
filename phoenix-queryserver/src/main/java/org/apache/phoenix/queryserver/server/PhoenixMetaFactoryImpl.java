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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.phoenix.queryserver.QueryServerOptions;
import org.apache.phoenix.queryserver.QueryServerProperties;

/**
 * Bridge between Phoenix and Avatica.
 */
public class PhoenixMetaFactoryImpl extends Configured implements PhoenixMetaFactory {


  // invoked via reflection
  public PhoenixMetaFactoryImpl() {
    super(HBaseConfiguration.create());
  }

  // invoked via reflection
  public PhoenixMetaFactoryImpl(Configuration conf) {
    super(conf);
  }

  @Override
  public Meta create(List<String> args) {
    Configuration conf = getConf();
    if (conf == null) {
      throw new NullPointerException(String.valueOf("Configuration must not be null."));
    }
    Properties info = new Properties();
    info.putAll(conf.getValByRegex("avatica.*"));
    try {
      final String url;
      if (args.size() == 0) {
        url = getConnectionUrl(info, conf);
      } else if (args.size() == 1) {
        url = args.get(0);
      } else {
        throw new RuntimeException(
            "0 or 1 argument expected. Received " + Arrays.toString(args.toArray()));
      }
      // TODO: what about -D configs passed in from cli? How do they get pushed down?
      return new JdbcMeta(url, info);
    } catch (SQLException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  //The following are copied and adopted from the phoenix-core QueryUtil and PhoenixEmbedded classes

  public final static String JDBC_PROTOCOL = "jdbc:phoenix";
  public final static char JDBC_PROTOCOL_SEPARATOR = ':';
  public final static char JDBC_PROTOCOL_TERMINATOR = ';';

  /**
   * @return connection url using the various properties set in props and conf.
   */
  public String getConnectionUrl(Properties props, Configuration conf)
     throws ClassNotFoundException, SQLException {
    // read the hbase properties from the configuration
    int port = getInt(HConstants.ZOOKEEPER_CLIENT_PORT, HConstants.DEFAULT_ZOOKEPER_CLIENT_PORT, props, conf);
    // Build the ZK quorum server string with "server:clientport" list, separated by ','
    final String server = getString(HConstants.ZOOKEEPER_QUORUM, HConstants.LOCALHOST, props, conf);
    String znodeParent = getString(HConstants.ZOOKEEPER_ZNODE_PARENT, HConstants.DEFAULT_ZOOKEEPER_ZNODE_PARENT, props, conf);
    String url = getUrl(server, port, znodeParent);
    if (url.endsWith(JDBC_PROTOCOL_TERMINATOR + "")) {
      url = url.substring(0, url.length() - 1);
    }
    // Mainly for testing to tack on the test=true part to ensure driver is found on server
    String defaultExtraArgs =
          conf != null
                  ? conf.get(QueryServerProperties.EXTRA_JDBC_ARGUMENTS_ATTRIB,
                      QueryServerOptions.DEFAULT_EXTRA_JDBC_ARGUMENTS)
                  : QueryServerOptions.DEFAULT_EXTRA_JDBC_ARGUMENTS;
    // If props doesn't have a default for extra args then use the extra args in conf as default
    String extraArgs =
          props.getProperty(QueryServerProperties.EXTRA_JDBC_ARGUMENTS_ATTRIB, defaultExtraArgs);
    if (extraArgs.length() > 0) {
      url += JDBC_PROTOCOL_TERMINATOR + extraArgs + JDBC_PROTOCOL_TERMINATOR;
    } else {
      url += JDBC_PROTOCOL_TERMINATOR;
    }
    return url;
  }

  private int getInt(String key, int defaultValue, Properties props, Configuration conf) {
    if (conf == null) {
      if (props == null) {
        throw new NullPointerException();
      }
      return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
    }
    return conf.getInt(key, defaultValue);
  }

  private String getString(String key, String defaultValue, Properties props, Configuration conf) {
      if (conf == null) {
        if (props == null) {
          throw new NullPointerException();
        }
        return props.getProperty(key, defaultValue);
      }
      return conf.get(key, defaultValue);
  }

  private static String getUrl(String zookeeperQuorum, Integer port, String rootNode) {
    return JDBC_PROTOCOL + JDBC_PROTOCOL_SEPARATOR
            + zookeeperQuorum + (port == null ? "" : ":" + port)
                  + (rootNode == null ? "" : ":" + rootNode)
                  + JDBC_PROTOCOL_TERMINATOR;
  }
}
