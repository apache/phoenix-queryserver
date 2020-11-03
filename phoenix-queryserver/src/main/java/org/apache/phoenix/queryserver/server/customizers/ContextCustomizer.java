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

import org.apache.calcite.avatica.server.ServerCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * A context based customizer
 */
public abstract class ContextCustomizer implements ServerCustomizer<Server> {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCustomizer.class);

    private final String contextPath;

    /**
     * @param contextPath The HTTP path which the repository will be hosted at
     */
    protected ContextCustomizer(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public void customize(Server server) {
        Handler[] handlers = server.getHandlers();
        if (handlers.length != 1) {
            LOG.warn("Observed handlers on server {}", Arrays.toString(handlers));
            throw new IllegalStateException("Expected to find one handler");
        }
        HandlerList list = (HandlerList) handlers[0];
        Handler[] realHandlers = list.getChildHandlers();
        Handler[] newHandlers = new Handler[realHandlers.length + 1];
        newHandlers[0] = createHandler(contextPath);
        System.arraycopy(realHandlers, 0, newHandlers, 1, realHandlers.length);
        server.setHandler(new HandlerList(newHandlers));
    }

    protected abstract Handler createHandler(String contextPath);
}
