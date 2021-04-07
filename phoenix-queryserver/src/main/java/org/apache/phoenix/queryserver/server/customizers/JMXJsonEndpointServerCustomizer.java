package org.apache.phoenix.queryserver.server.customizers;

import org.apache.calcite.avatica.server.ServerCustomizer;
import org.apache.hadoop.jmx.JMXJsonServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class JMXJsonEndpointServerCustomizer implements ServerCustomizer<Server> {
  private static final Logger LOG = LoggerFactory.getLogger(JMXJsonEndpointServerCustomizer.class);

  @Override
  public void customize(Server server) {
    Handler[] handlers = server.getHandlers();
    if (handlers.length != 1) {
      LOG.warn("Observed handlers on server {}", Arrays.toString(handlers));
      throw new IllegalStateException("Expected to find one handler");
    }
    HandlerList list = (HandlerList) handlers[0];

    WebAppContext ctx = new WebAppContext();
    ctx.setContextPath("/");
    ServletHolder holder = new ServletHolder(JMXJsonServlet.class);
    ctx.addServlet(holder, "/jmx");

    ctx.setServer(server);

    Handler[] realHandlers = list.getChildHandlers();

    Handler[] newHandlers = new Handler[realHandlers.length + 1];
    newHandlers[0] = ctx;
    System.arraycopy(realHandlers, 0, newHandlers, 1, realHandlers.length);
    server.setHandler(new HandlerList(newHandlers));
  }
}
