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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class HostedClientJarsServerCustomizerTest {

  @Test
  public void testHandlerIsPrefixed() {
    final Handler handler1 = Mockito.mock(Handler.class);
    final Handler handler2 = Mockito.mock(Handler.class);

    Server svr = new Server();
    svr.setHandler(new HandlerList(handler1, handler2));

    File f = new File("/for-test");
    String context = "/my-context";
    HostedClientJarsServerCustomizer customizer = new HostedClientJarsServerCustomizer(f, context);
    customizer.customize(svr);

    assertEquals(1, svr.getHandlers().length);
    Handler actualHandler = svr.getHandler();
    assertTrue(actualHandler instanceof HandlerList,"Handler was " + actualHandler.getClass());

    HandlerList actualHandlerList = (HandlerList) actualHandler;
    assertEquals(3, actualHandlerList.getHandlers().length);
    assertEquals(handler1, actualHandlerList.getHandlers()[1]);
    assertEquals(handler2, actualHandlerList.getHandlers()[2]);

    Handler injectedHandler = actualHandlerList.getHandlers()[0];
    assertTrue(injectedHandler instanceof ContextHandler,"Handler was " + injectedHandler.getClass());
    ContextHandler ctx = (ContextHandler) injectedHandler;
    assertTrue(ctx.getHandler() instanceof ResourceHandler, "Handler was " + ctx.getHandler().getClass());
    assertEquals(context, ctx.getContextPath());
    ResourceHandler res = (ResourceHandler) ctx.getHandler();
    // Jetty puts in a proper URI for the file we give it
    assertEquals("file://" + f.getAbsolutePath(), res.getResourceBase());
  }

}
