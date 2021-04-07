/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.apache.phoenix.queryserver;

import static org.apache.phoenix.queryserver.QueryServerProperties.PHOENIX_QUERY_SERVER_CLUSTER_BASE_PATH;
import static org.apache.phoenix.queryserver.QueryServerProperties.PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED;
import static org.apache.phoenix.queryserver.QueryServerProperties.PHOENIX_QUERY_SERVER_SERVICE_NAME;
import static org.apache.phoenix.queryserver.QueryServerProperties.PHOENIX_QUERY_SERVER_ZK_ACL_PASSWORD;
import static org.apache.phoenix.queryserver.QueryServerProperties.PHOENIX_QUERY_SERVER_ZK_ACL_USERNAME;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

public class QueryServerOptions {

    // QueryServer defaults -- ensure ThinClientUtil is also updated since
    // phoenix-queryserver-client
    // doesn't depend on phoenix-core.
    public static final String DEFAULT_QUERY_SERVER_SERIALIZATION = "PROTOBUF";
    public static final int DEFAULT_QUERY_SERVER_HTTP_PORT = 8765;
    public static final long DEFAULT_QUERY_SERVER_UGI_CACHE_MAX_SIZE = 1000L;
    public static final int DEFAULT_QUERY_SERVER_UGI_CACHE_INITIAL_SIZE = 100;
    public static final int DEFAULT_QUERY_SERVER_UGI_CACHE_CONCURRENCY = 10;
    public static final boolean DEFAULT_QUERY_SERVER_SPNEGO_AUTH_DISABLED = false;
    public static final boolean DEFAULT_QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR = false;
    public static final boolean DEFAULT_QUERY_SERVER_CUSTOM_AUTH_ENABLED = false;
    public static final String DEFAULT_QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM = "doAs";
    public static final boolean DEFAULT_QUERY_SERVER_DISABLE_KERBEROS_LOGIN = false;
    public static final boolean DEFAULT_QUERY_SERVER_JMXJSONENDPOINT_DISABLED = false;

    public static final boolean DEFAULT_QUERY_SERVER_TLS_ENABLED = false;
    //We default to empty *store password
    public static final String DEFAULT_QUERY_SERVER_TLS_KEYSTORE_PASSWORD = "";
    public static final String DEFAULT_QUERY_SERVER_TLS_TRUSTSTORE_PASSWORD = "";

    @SuppressWarnings("serial")
    public static final Set<String> DEFAULT_QUERY_SERVER_SKIP_WORDS = new HashSet<String>() {
        {
            add("secret");
            add("passwd");
            add("password");
            add("credential");
        }
    };

    // Loadbalancer defaults
    public static final boolean DEFAULT_PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED = false;
    public static final String DEFAULT_PHOENIX_QUERY_SERVER_CLUSTER_BASE_PATH = "/phoenix";
    public static final String DEFAULT_PHOENIX_QUERY_SERVER_SERVICE_NAME = "queryserver";
    public static final String DEFAULT_PHOENIX_QUERY_SERVER_ZK_ACL_USERNAME = "phoenix";
    public static final String DEFAULT_PHOENIX_QUERY_SERVER_ZK_ACL_PASSWORD = "phoenix";

    // Maven repo defaults
    public static final boolean DEFAULT_CLIENT_JARS_ENABLED = false;
    public static final String DEFAULT_CLIENT_JARS_REPO = "";
    public static final String DEFAULT_CLIENT_JARS_CONTEXT = "/maven";

    // Common defaults
    public static final String DEFAULT_EXTRA_JDBC_ARGUMENTS = "";

    private final Configuration config;

    private QueryServerOptions(Configuration config) {
        this.config = config;
    }

    public static QueryServerOptions withDefaults() {
        Configuration config = HBaseConfiguration.create();
        QueryServerOptions options =
                new QueryServerOptions(config)
                        .setIfUnset(PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED,
                            DEFAULT_PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED)
                        .setIfUnset(PHOENIX_QUERY_SERVER_CLUSTER_BASE_PATH,
                            DEFAULT_PHOENIX_QUERY_SERVER_CLUSTER_BASE_PATH)
                        .setIfUnset(PHOENIX_QUERY_SERVER_SERVICE_NAME,
                            DEFAULT_PHOENIX_QUERY_SERVER_SERVICE_NAME)
                        .setIfUnset(PHOENIX_QUERY_SERVER_ZK_ACL_USERNAME,
                            DEFAULT_PHOENIX_QUERY_SERVER_ZK_ACL_USERNAME)
                        .setIfUnset(PHOENIX_QUERY_SERVER_ZK_ACL_PASSWORD,
                            DEFAULT_PHOENIX_QUERY_SERVER_ZK_ACL_PASSWORD);

        return options;
    }

    public Configuration getConfiguration() {
        return config;
    }

    private QueryServerOptions setIfUnset(String name, int value) {
        config.setIfUnset(name, Integer.toString(value));
        return this;
    }

    private QueryServerOptions setIfUnset(String name, boolean value) {
        config.setIfUnset(name, Boolean.toString(value));
        return this;
    }

    private QueryServerOptions setIfUnset(String name, long value) {
        config.setIfUnset(name, Long.toString(value));
        return this;
    }

    private QueryServerOptions setIfUnset(String name, String value) {
        config.setIfUnset(name, value);
        return this;
    }

}
