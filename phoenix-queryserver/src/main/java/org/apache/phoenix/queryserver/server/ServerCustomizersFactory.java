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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.calcite.avatica.server.AvaticaServerConfiguration;
import org.apache.calcite.avatica.server.ServerCustomizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.queryserver.QueryServerOptions;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.server.customizers.HostedClientJarsServerCustomizer;
import org.apache.phoenix.queryserver.server.customizers.JMXJsonEndpointServerCustomizer;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates customizers for the underlying Avatica HTTP server.
 * Allows for fine grained control of authentication, etc.
 */
public interface ServerCustomizersFactory {
    /**
     * Creates a list of customizers that will customize the server.
     * @param conf Configuration to use
     * @param avaticaServerConfiguration to use in case custom-auth is enabled
     * @return List of server suctomizers
     */
    List<ServerCustomizer<Server>> createServerCustomizers(Configuration conf, AvaticaServerConfiguration avaticaServerConfiguration);

    /**
     * Factory that creates an empty list of customizers.
     */
    class ServerCustomizersFactoryImpl implements ServerCustomizersFactory {
        private static final Logger LOG = LoggerFactory.getLogger(ServerCustomizersFactoryImpl.class);
        @Override
        public List<ServerCustomizer<Server>> createServerCustomizers(Configuration conf,
                                                                      AvaticaServerConfiguration avaticaServerConfiguration) {
            List<ServerCustomizer<Server>> customizers = new ArrayList<>();
            if (conf.getBoolean(QueryServerProperties.CLIENT_JARS_ENABLED_ATTRIB, QueryServerOptions.DEFAULT_CLIENT_JARS_ENABLED)) {
                String repoLocation = conf.get(QueryServerProperties.CLIENT_JARS_REPO_ATTRIB,
                    QueryServerOptions.DEFAULT_CLIENT_JARS_REPO);
                if (repoLocation != null && !repoLocation.isEmpty()) {
                    File repo = new File(repoLocation);
                    if (!repo.isDirectory()) {
                        throw new IllegalArgumentException("Provided maven repository is not a directory. " + repo);
                    }
                    String contextPath = conf.get(QueryServerProperties.CLIENT_JARS_CONTEXT_ATTRIB,
                        QueryServerOptions.DEFAULT_CLIENT_JARS_CONTEXT);
                    LOG.info("Creating ServerCustomizer to host client jars from {} at HTTP endpoint {}", repo, contextPath);
                    HostedClientJarsServerCustomizer customizer = new HostedClientJarsServerCustomizer(repo, contextPath);
                    customizers.add(customizer);
                } else {
                    LOG.warn("Empty value provided for {}, ignoring", QueryServerProperties.CLIENT_JARS_REPO_ATTRIB);
                }
            }
            if (!conf.getBoolean(QueryServerProperties.QUERY_SERVER_JMX_JSON_ENDPOINT_DISABLED,
                QueryServerOptions.DEFAULT_QUERY_SERVER_JMXJSONENDPOINT_DISABLED)) {
                customizers.add(new JMXJsonEndpointServerCustomizer());
            }
            return Collections.unmodifiableList(customizers);
        }
    }
}
