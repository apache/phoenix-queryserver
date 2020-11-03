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
package org.apache.phoenix.queryserver.server.customizers;

import java.io.File;
import java.util.Arrays;

import org.apache.calcite.avatica.server.ServerCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hosts a Maven repository from local filesystem over HTTP from within PQS.
 */
public class HostedClientJarsServerCustomizer extends ContextCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(HostedClientJarsServerCustomizer.class);

    private final File repoRoot;

    /**
     * @param localMavenRepoRoot The path to the Phoenix-built maven repository on the local filesystem
     * @param contextPath The HTTP path which the repository will be hosted at
     */
    public HostedClientJarsServerCustomizer(File localMavenRepoRoot, String contextPath) {
        super(contextPath);
      this.repoRoot = localMavenRepoRoot;
    }

    @Override
    protected Handler createHandler(String contextPath) {
        ContextHandler ctx = new ContextHandler(contextPath);
        ResourceHandler resource = new ResourceHandler();
        resource.setDirAllowed(true);
        resource.setDirectoriesListed(false);
        resource.setResourceBase(repoRoot.getAbsolutePath());
        ctx.setHandler(resource);
        return ctx;
    }
}
