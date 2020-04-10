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

import java.util.Collections;
import java.util.List;

import org.apache.calcite.avatica.server.AvaticaServerConfiguration;
import org.apache.calcite.avatica.server.ServerCustomizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.queryserver.server.ServerCustomizersFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Avatica ServerCustomizer which performs HTTP Basic authentication against a static user database.
 *
 * For testing ONLY.
 */
public class BasicAuthenticationServerCustomizer implements ServerCustomizer<Server> {
  private static final Logger LOG = LoggerFactory.getLogger(BasicAuthenticationServerCustomizer.class);

  public static final String USER_AUTHORIZED = "user3";
  public static final String USER_PW = "s3cr3t";

  public static class BasicAuthServerCustomizerFactory implements ServerCustomizersFactory {
    @Override
    public List<ServerCustomizer<Server>> createServerCustomizers(
            Configuration conf, AvaticaServerConfiguration avaticaServerConfiguration) {
        return Collections.<ServerCustomizer<Server>>singletonList(new BasicAuthenticationServerCustomizer());
    }
  }

  @Override
  public void customize(Server server) {
      LOG.debug("Customizing server to allow requests for {}", USER_AUTHORIZED);

      UserStore store = new UserStore();
      store.addUser(USER_AUTHORIZED, Credential.getCredential(USER_PW), new String[] {"users"});
      HashLoginService login = new HashLoginService();
      login.setName("users");
      login.setUserStore(store);

      Constraint constraint = new Constraint();
      constraint.setName(Constraint.__BASIC_AUTH);
      constraint.setRoles(new String[]{"users"});
      constraint.setAuthenticate(true);

      ConstraintMapping cm = new ConstraintMapping();
      cm.setConstraint(constraint);
      cm.setPathSpec("/*");

      ConstraintSecurityHandler security = new ConstraintSecurityHandler();
      security.setAuthenticator(new BasicAuthenticator());
      security.setRealmName("users");
      security.addConstraintMapping(cm);
      security.setLoginService(login);

      // chain the PQS handler to security
      security.setHandler(server.getHandlers()[0]);
      server.setHandler(security);
  }
}
