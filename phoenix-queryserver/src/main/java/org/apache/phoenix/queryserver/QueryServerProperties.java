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

public class QueryServerProperties {

    // queryserver configuration keys
    public static final String QUERY_SERVER_SERIALIZATION_ATTRIB =
            "phoenix.queryserver.serialization";
    public static final String QUERY_SERVER_META_FACTORY_ATTRIB =
            "phoenix.queryserver.metafactory.class";
    public static final String QUERY_SERVER_HTTP_PORT_ATTRIB = "phoenix.queryserver.http.port";
    public static final String QUERY_SERVER_ENV_LOGGING_ATTRIB =
            "phoenix.queryserver.envvars.logging.disabled";
    public static final String QUERY_SERVER_ENV_LOGGING_SKIPWORDS_ATTRIB =
            "phoenix.queryserver.envvars.logging.skipwords";
    public static final String QUERY_SERVER_KEYTAB_FILENAME_ATTRIB =
            "phoenix.queryserver.keytab.file";
    public static final String QUERY_SERVER_HTTP_KEYTAB_FILENAME_ATTRIB =
            "phoenix.queryserver.http.keytab.file";
    public static final String QUERY_SERVER_KERBEROS_PRINCIPAL_ATTRIB =
            "phoenix.queryserver.kerberos.principal";
    public static final String QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB_LEGACY =
            "phoenix.queryserver.kerberos.http.principal";
    public static final String QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB =
            "phoenix.queryserver.http.kerberos.principal";
    public static final String QUERY_SERVER_DNS_NAMESERVER_ATTRIB =
            "phoenix.queryserver.dns.nameserver";
    public static final String QUERY_SERVER_DNS_INTERFACE_ATTRIB =
            "phoenix.queryserver.dns.interface";
    public static final String QUERY_SERVER_HBASE_SECURITY_CONF_ATTRIB =
            "hbase.security.authentication";
    public static final String QUERY_SERVER_HADOOP_SECURITY_CONF_ATTRIB =
            "hadoop.security.authentication";
    public static final String QUERY_SERVER_UGI_CACHE_MAX_SIZE =
            "phoenix.queryserver.ugi.cache.max.size";
    public static final String QUERY_SERVER_UGI_CACHE_INITIAL_SIZE =
            "phoenix.queryserver.ugi.cache.initial.size";
    public static final String QUERY_SERVER_UGI_CACHE_CONCURRENCY =
            "phoenix.queryserver.ugi.cache.concurrency";
    public static final String QUERY_SERVER_KERBEROS_ALLOWED_REALMS =
            "phoenix.queryserver.kerberos.allowed.realms";
    public static final String QUERY_SERVER_SPNEGO_AUTH_DISABLED_ATTRIB =
            "phoenix.queryserver.spnego.auth.disabled";
    public static final String QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR_ATTRIB =
            "phoenix.queryserver.withRemoteUserExtractor";
    public static final String QUERY_SERVER_CUSTOM_AUTH_ENABLED =
            "phoenix.queryserver.custom.auth.enabled";
    public static final String QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM =
            "phoenix.queryserver.remoteUserExtractor.param";
    public static final String QUERY_SERVER_DISABLE_KERBEROS_LOGIN =
            "phoenix.queryserver.disable.kerberos.login";
    public static final String QUERY_SERVER_TLS_KEYSTORE_TYPE_KEY =
            "phoenix.queryserver.tls.keystore.type";
    public static final String QUERY_SERVER_TLS_KEYSTORE_TYPE_DEFAULT =
            "jks";
    public static final String QUERY_SERVER_TLS_ENABLED =
            "phoenix.queryserver.tls.enabled";
    public static final String QUERY_SERVER_TLS_KEYSTORE =
            "phoenix.queryserver.tls.keystore";
    public static final String QUERY_SERVER_TLS_KEYSTORE_PASSWORD =
            "phoenix.queryserver.tls.keystore.password";
    public static final String QUERY_SERVER_TLS_TRUSTSTORE =
            "phoenix.queryserver.tls.truststore";
    public static final String QUERY_SERVER_TLS_TRUSTSTORE_PASSWORD =
            "phoenix.queryserver.tls.truststore.password";
    public static final String QUERY_SERVER_JMX_JSON_ENDPOINT_DISABLED =
            "phoenix.queryserver.jmxjsonendpoint.disabled";

    // keys for load balancer
    public static final String PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED =
            "phoenix.queryserver.loadbalancer.enabled";
    public static final String PHOENIX_QUERY_SERVER_CLUSTER_BASE_PATH =
            "phoenix.queryserver.base.path";
    public static final String PHOENIX_QUERY_SERVER_SERVICE_NAME =
            "phoenix.queryserver.service.name";
    public static final String PHOENIX_QUERY_SERVER_ZK_ACL_USERNAME =
            "phoenix.queryserver.zookeeper.acl.username";
    public static final String PHOENIX_QUERY_SERVER_ZK_ACL_PASSWORD =
            "phoenix.queryserver.zookeeper.acl.password";

    // keys that are also used by phoenix-core
    public static final String ZOOKEEPER_QUORUM_ATTRIB = "hbase.zookeeper.quorum";
    public static final String ZOOKEEPER_PORT_ATTRIB = "hbase.zookeeper.property.clientPort";
    public static final String EXTRA_JDBC_ARGUMENTS_ATTRIB = "phoenix.jdbc.extra.arguments";

    public static final String CLIENT_JARS_ENABLED_ATTRIB = "phoenix.queryserver.client.jars.enabled";
    public static final String CLIENT_JARS_REPO_ATTRIB = "phoenix.queryserver.client.jars.repo";
    public static final String CLIENT_JARS_CONTEXT_ATTRIB = "phoenix.queryserver.client.jars.context";
}
