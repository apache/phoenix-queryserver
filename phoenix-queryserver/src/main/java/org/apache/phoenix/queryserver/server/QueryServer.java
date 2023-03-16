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

import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.remote.Service;
import org.apache.calcite.avatica.server.AvaticaServerConfiguration;
import org.apache.calcite.avatica.server.DoAsRemoteUserCallback;
import org.apache.calcite.avatica.server.HttpServer;
import org.apache.calcite.avatica.server.HttpServer.Builder;
import org.apache.calcite.avatica.server.RemoteUserExtractor;
import org.apache.calcite.avatica.server.RemoteUserExtractionException;
import org.apache.calcite.avatica.server.HttpRequestRemoteUserExtractor;
import org.apache.calcite.avatica.server.HttpQueryStringParameterRemoteUserExtractor;
import org.apache.calcite.avatica.server.ServerCustomizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Strings;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.ProxyUsers;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.phoenix.loadbalancer.service.LoadBalanceZookeeperConf;
import org.apache.phoenix.queryserver.QueryServerOptions;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.register.Registry;
import org.apache.phoenix.util.InstanceResolver;
import org.apache.phoenix.util.SimpleLRUCache;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

/**
 * A query server for Phoenix over Calcite's Avatica.
 */
public final class QueryServer extends Configured implements Tool, Runnable {

  protected static final Logger LOG = LoggerFactory.getLogger(QueryServer.class);

  private final String[] argv;
  private final CountDownLatch runningLatch = new CountDownLatch(1);
  private HttpServer server = null;
  private int retCode = 0;
  private Throwable t = null;
  private Registry registry;

  /**
   * Log information about the currently running JVM.
   */
  public static void logJVMInfo() {
    // Print out vm stats before starting up.
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    if (runtime != null) {
      LOG.info("vmName=" + runtime.getVmName() + ", vmVendor=" +
              runtime.getVmVendor() + ", vmVersion=" + runtime.getVmVersion());
      LOG.info("vmInputArguments=" + runtime.getInputArguments());
    }
  }

  /**
   * Logs information about the currently running JVM process including
   * the environment variables. Logging of env vars can be disabled by
   * setting {@code "phoenix.envvars.logging.disabled"} to {@code "true"}.
   * <p>If enabled, you can also exclude environment variables containing
   * certain substrings by setting {@code "phoenix.envvars.logging.skipwords"}
   * to comma separated list of such substrings.
   */
  public static void logProcessInfo(Configuration conf) {
    // log environment variables unless asked not to
    if (conf == null || !conf.getBoolean(QueryServerProperties.QUERY_SERVER_ENV_LOGGING_ATTRIB, false)) {
      Set<String> skipWords = new HashSet<String>(
          QueryServerOptions.DEFAULT_QUERY_SERVER_SKIP_WORDS);
      if (conf != null) {
        String[] confSkipWords = conf.getStrings(
            QueryServerProperties.QUERY_SERVER_ENV_LOGGING_SKIPWORDS_ATTRIB);
        if (confSkipWords != null) {
          skipWords.addAll(Arrays.asList(confSkipWords));
        }
      }

      nextEnv:
      for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
        String key = entry.getKey().toLowerCase();
        String value = entry.getValue().toLowerCase();
        // exclude variables which may contain skip words
        for(String skipWord : skipWords) {
          if (key.contains(skipWord) || value.contains(skipWord))
            continue nextEnv;
        }
        LOG.info("env:"+entry);
      }
    }
    // and JVM info
    logJVMInfo();
  }

  /** Constructor for use from {@link org.apache.hadoop.util.ToolRunner}. */
  public QueryServer() {
    this(null, null);
  }

  /** Constructor for use as {@link java.lang.Runnable}. */
  public QueryServer(String[] argv, Configuration conf) {
    this.argv = argv;
    setConf(conf);
  }

  /**
   * @return the port number this instance is bound to, or {@code -1} if the server is not running.
   */
  //@VisibleForTesting
  public int getPort() {
    if (server == null) return -1;
    return server.getPort();
  }

  /**
   * @return the return code from running as a {@link Tool}.
   */
  //@VisibleForTesting
  public int getRetCode() {
    return retCode;
  }

  /**
   * @return the throwable from an unsuccessful run, or null otherwise.
   */
  //@VisibleForTesting
  public Throwable getThrowable() {
    return t;
  }

  /** Calling thread waits until the server is running. */
  public void awaitRunning() throws InterruptedException {
    runningLatch.await();
  }

  /** Calling thread waits until the server is running. */
  public void awaitRunning(long timeout, TimeUnit unit) throws InterruptedException {
    runningLatch.await(timeout, unit);
  }

  @Override
  public int run(String[] args) throws Exception {
    logProcessInfo(getConf());
    final boolean loadBalancerEnabled = getConf().getBoolean(QueryServerProperties.PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED,
            QueryServerOptions.DEFAULT_PHOENIX_QUERY_SERVER_LOADBALANCER_ENABLED);
    try {
      final boolean isKerberos = "kerberos".equalsIgnoreCase(getConf().get(
          QueryServerProperties.QUERY_SERVER_HBASE_SECURITY_CONF_ATTRIB));
      final boolean isHadoopKerberos = "kerberos".equalsIgnoreCase(getConf().get(
          QueryServerProperties.QUERY_SERVER_HADOOP_SECURITY_CONF_ATTRIB));
      final boolean disableSpnego = getConf().getBoolean(QueryServerProperties.QUERY_SERVER_SPNEGO_AUTH_DISABLED_ATTRIB,
              QueryServerOptions.DEFAULT_QUERY_SERVER_SPNEGO_AUTH_DISABLED);
      String hostname;
      final boolean disableLogin = getConf().getBoolean(QueryServerProperties.QUERY_SERVER_DISABLE_KERBEROS_LOGIN,
              QueryServerOptions.DEFAULT_QUERY_SERVER_DISABLE_KERBEROS_LOGIN);

      // handle secure cluster credentials
      if (isKerberos && !disableLogin) {
        if(!isHadoopKerberos) {
          LOG.error("HBase and Hadoop security config inconsistent, "
                  + QueryServerProperties.QUERY_SERVER_HBASE_SECURITY_CONF_ATTRIB
                  + " was configured as kerberos, but "
                  + QueryServerProperties.QUERY_SERVER_HADOOP_SECURITY_CONF_ATTRIB + " not!");
          return -1;
        }
        hostname = Strings.domainNamePointerToHostName(DNS.getDefaultHost(
            getConf().get(QueryServerProperties.QUERY_SERVER_DNS_INTERFACE_ATTRIB, "default"),
            getConf().get(QueryServerProperties.QUERY_SERVER_DNS_NAMESERVER_ATTRIB, "default")));
        if (LOG.isDebugEnabled()) {
          LOG.debug("Login to " + hostname + " using " + getConf().get(
              QueryServerProperties.QUERY_SERVER_KEYTAB_FILENAME_ATTRIB)
              + " and principal " + getConf().get(
                  QueryServerProperties.QUERY_SERVER_KERBEROS_PRINCIPAL_ATTRIB) + ".");
        }
        SecurityUtil.login(getConf(), QueryServerProperties.QUERY_SERVER_KEYTAB_FILENAME_ATTRIB,
            QueryServerProperties.QUERY_SERVER_KERBEROS_PRINCIPAL_ATTRIB, hostname);
        LOG.info("Kerberos login successful.");
      } else {
        hostname = InetAddress.getLocalHost().getHostName();
        LOG.info("Kerberos is off and hostname is : " + hostname);
      }

      int port = getConf().getInt(QueryServerProperties.QUERY_SERVER_HTTP_PORT_ATTRIB,
          QueryServerOptions.DEFAULT_QUERY_SERVER_HTTP_PORT);
      LOG.debug("Listening on port " + port);

      // Update proxyuser configuration for impersonation
      ProxyUsers.refreshSuperUserGroupsConfiguration(getConf());

      // Start building the Avatica HttpServer
      final HttpServer.Builder<Server>
              builder =
              HttpServer.Builder.<Server>newBuilder().withPort(port);

      UserGroupInformation ugi = getUserGroupInformation();

      AvaticaServerConfiguration avaticaServerConfiguration = null;

      // RemoteUserCallbacks and RemoteUserExtractor are part of AvaticaServerConfiguration
      // Hence they should be customizable when using QUERY_SERVER_CUSTOM_AUTH_ENABLED
      // Handlers should be customized via ServerCustomizers
      if (getConf().getBoolean(QueryServerProperties.QUERY_SERVER_CUSTOM_AUTH_ENABLED,
          QueryServerOptions.DEFAULT_QUERY_SERVER_CUSTOM_AUTH_ENABLED)) {
        avaticaServerConfiguration = enableCustomAuth(builder, getConf(), ugi);
      } else {
        if (isKerberos) {
          // Enable client auth when using Kerberos auth for HBase
          configureClientAuthentication(builder, disableSpnego, ugi);
        }
        setRemoteUserExtractorIfNecessary(builder, getConf());
        //Avatica doesn't support TLS with custom auth (Why?), hence we only set it in this branch
        setTlsIfNeccessary(builder, getConf());
        setHandler(args, builder);
      }

      enableServerCustomizersIfNecessary(builder, getConf(), avaticaServerConfiguration);

      // Build and start the HttpServer
      server = builder.build();
      server.start();
      if (loadBalancerEnabled) {
        registerToServiceProvider(hostname);
      }
      runningLatch.countDown();
      server.join();
      return 0;
    } catch (Throwable t) {
      LOG.error("Unrecoverable service error. Shutting down.", t);
      this.t = t;
      return -1;
    } finally {
      if (loadBalancerEnabled) {
        unRegister();
      }
    }
  }

  private void setTlsIfNeccessary(Builder<Server> builder, Configuration conf) throws Exception {
    final boolean useTls = getConf().getBoolean(QueryServerProperties.QUERY_SERVER_TLS_ENABLED, QueryServerOptions.DEFAULT_QUERY_SERVER_TLS_ENABLED);
    if(useTls) {
      final String tlsKeystore = getConf().get(QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE);
      final String keystoreType = getConf().get(QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE_TYPE_KEY, QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE_TYPE_DEFAULT);

      final String tlsKeystorePassword = getConf().get(QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE_PASSWORD, QueryServerOptions.DEFAULT_QUERY_SERVER_TLS_KEYSTORE_PASSWORD);
      final String tlsTruststore = getConf().get(QueryServerProperties.QUERY_SERVER_TLS_TRUSTSTORE);
      final String tlsTruststorePassword = getConf().get(QueryServerProperties.QUERY_SERVER_TLS_TRUSTSTORE_PASSWORD, QueryServerOptions.DEFAULT_QUERY_SERVER_TLS_TRUSTSTORE_PASSWORD);
      if(tlsKeystore == null) {
        throw new Exception(String.format("if %s is enabled, %s must be specfified" , QueryServerProperties.QUERY_SERVER_TLS_ENABLED, QueryServerProperties.QUERY_SERVER_TLS_KEYSTORE));
      }
      final File tlsKeystoreFile = new File(tlsKeystore);
      if(tlsTruststore == null) {
        throw new Exception(String.format("if %s is enabled, %s must be specfified" , QueryServerProperties.QUERY_SERVER_TLS_ENABLED, QueryServerProperties.QUERY_SERVER_TLS_TRUSTSTORE));
      }
      final File tlsTruststoreFile = new File(tlsTruststore);
      builder.withTLS(tlsKeystoreFile, tlsKeystorePassword, tlsTruststoreFile, tlsTruststorePassword, keystoreType);
    }
}

  //@VisibleForTesting
  void configureClientAuthentication(final HttpServer.Builder builder, boolean disableSpnego, UserGroupInformation ugi) throws IOException {

    // Enable SPNEGO for client authentication unless it's explicitly disabled
    if (!disableSpnego) {
      configureSpnegoAuthentication(builder, ugi);
    }
    configureCallBack(builder, ugi);
  }

  //@VisibleForTesting
  void configureSpnegoAuthentication(HttpServer.Builder builder, UserGroupInformation ugi) throws IOException {
    String keytabPath = getConf().get(QueryServerProperties.QUERY_SERVER_KEYTAB_FILENAME_ATTRIB);
    File keytab = new File(keytabPath);
    String httpKeytabPath =
            getConf().get(QueryServerProperties.QUERY_SERVER_HTTP_KEYTAB_FILENAME_ATTRIB, null);
    String httpPrincipal = getSpnegoPrincipal(getConf());
    File httpKeytab = null;
    if (null != httpKeytabPath) {
        httpKeytab = new File(httpKeytabPath);
    }

    String realmsString = getConf().get(QueryServerProperties.QUERY_SERVER_KERBEROS_ALLOWED_REALMS, null);
    String[] additionalAllowedRealms = null;
    if (null != realmsString) {
      additionalAllowedRealms = StringUtils.split(realmsString, ',');
    }
    if (null != httpKeytabPath && null != httpPrincipal) {
      builder.withSpnego(httpPrincipal, additionalAllowedRealms).withAutomaticLogin(httpKeytab);
    } else {
      builder.withSpnego(ugi.getUserName(), additionalAllowedRealms)
              .withAutomaticLogin(keytab);
    }
  }

  /**
   * Returns the Kerberos principal to use for SPNEGO, substituting {@code _HOST}
   * if it is present as the "instance" component of the Kerberos principal. It returns
   * the configured principal as-is if {@code _HOST} is not the "instance".
   */
  String getSpnegoPrincipal(Configuration conf) throws IOException {
    String httpPrincipal = conf.get(
        QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB, null);
    // Backwards compat for a configuration key change
    if (httpPrincipal == null) {
      httpPrincipal = conf.get(
          QueryServerProperties.QUERY_SERVER_KERBEROS_HTTP_PRINCIPAL_ATTRIB_LEGACY, null);
    }

    String hostname = Strings.domainNamePointerToHostName(DNS.getDefaultHost(
        conf.get(QueryServerProperties.QUERY_SERVER_DNS_INTERFACE_ATTRIB, "default"),
        conf.get(QueryServerProperties.QUERY_SERVER_DNS_NAMESERVER_ATTRIB, "default")));
    return SecurityUtil.getServerPrincipal(httpPrincipal, hostname);
  }

  //@VisibleForTesting
  UserGroupInformation getUserGroupInformation() throws IOException {
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    LOG.debug("Current user is " + ugi);
    if (!ugi.hasKerberosCredentials()) {
      ugi = UserGroupInformation.getLoginUser();
      LOG.debug("Current user does not have Kerberos credentials, using instead " + ugi);
    }
    return ugi;
  }

  //@VisibleForTesting
  void configureCallBack(HttpServer.Builder<Server> builder, UserGroupInformation ugi) {
    builder.withImpersonation(new PhoenixDoAsCallback(ugi, getConf()));
  }

  private void setHandler(String[] args, HttpServer.Builder<Server> builder) throws Exception {
    Class<? extends PhoenixMetaFactory> factoryClass = getConf().getClass(
            QueryServerProperties.QUERY_SERVER_META_FACTORY_ATTRIB, PhoenixMetaFactoryImpl.class,
            PhoenixMetaFactory.class);
    PhoenixMetaFactory factory =
            factoryClass.getDeclaredConstructor(Configuration.class).newInstance(getConf());
    Meta meta = factory.create(Arrays.asList(args));
    Service service = new LocalService(meta);
    builder.withHandler(service, getSerialization(getConf()));
  }

  public synchronized void stop() {
    server.stop();
  }

  public boolean registerToServiceProvider(String hostName)  {

    boolean success = true ;
    try {
      LoadBalanceZookeeperConf loadBalanceConfiguration = getLoadBalanceConfiguration();
      if (loadBalanceConfiguration == null) {
        throw new NullPointerException();
      }
      this.registry = getRegistry();

      if (registry == null) {
        throw new NullPointerException();
      }
      String zkConnectString = loadBalanceConfiguration.getZkConnectString();
      this.registry.registerServer(loadBalanceConfiguration, getPort(), zkConnectString, hostName);
    } catch(Throwable ex){
      LOG.debug("Caught an error trying to register with the load balancer", ex);
      success = false;
    }
    return success;
  }


  public LoadBalanceZookeeperConf getLoadBalanceConfiguration()  {
    ServiceLoader<LoadBalanceZookeeperConf> serviceLocator= ServiceLoader.load(LoadBalanceZookeeperConf.class);
    LoadBalanceZookeeperConf zookeeperConfig = null;
    try {
      if (serviceLocator.iterator().hasNext())
        zookeeperConfig = serviceLocator.iterator().next();
    } catch(ServiceConfigurationError ex) {
      LOG.debug("Unable to locate the service provider for load balancer configuration", ex);
    }
    return zookeeperConfig;
  }

  public Registry getRegistry()  {
    ServiceLoader<Registry> serviceLocator= ServiceLoader.load(Registry.class);
    Registry registry = null;
    try {
      if (serviceLocator.iterator().hasNext())
        registry = serviceLocator.iterator().next();
    } catch(ServiceConfigurationError ex) {
      LOG.debug("Unable to locate the zookeeper registry for the load balancer", ex);
    }
    return registry;
  }

  public boolean unRegister()  {
    boolean success = true;
    try {
      registry.unRegisterServer();
    }catch(Throwable ex) {
      LOG.debug("Caught an error while de-registering the query server from the load balancer",ex);
      success = false;
    }
    return success;
  }
  /**
   * Parses the serialization method from the configuration.
   *
   * @param conf The configuration to parse
   * @return The Serialization method
   */
  Driver.Serialization getSerialization(Configuration conf) {
    String serializationName = conf.get(QueryServerProperties.QUERY_SERVER_SERIALIZATION_ATTRIB,
        QueryServerOptions.DEFAULT_QUERY_SERVER_SERIALIZATION);

    Driver.Serialization serialization;
    // Otherwise, use what was provided in the configuration
    try {
      serialization = Driver.Serialization.valueOf(serializationName);
    } catch (Exception e) {
      LOG.error("Unknown message serialization type for " + serializationName);
      throw e;
    }

    return serialization;
  }

  @Override public void run() {
    try {
      retCode = run(argv);
    } catch (Exception e) {
      // already logged
    }
  }

  // add remoteUserExtractor to builder if enabled
  //@VisibleForTesting
  public void setRemoteUserExtractorIfNecessary(HttpServer.Builder builder, Configuration conf) {
    if (conf.getBoolean(QueryServerProperties.QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR_ATTRIB,
            QueryServerOptions.DEFAULT_QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR)) {
      builder.withRemoteUserExtractor(createRemoteUserExtractor(conf));
    }
  }

  //@VisibleForTesting
  public void enableServerCustomizersIfNecessary(HttpServer.Builder<Server> builder,
                                                 Configuration conf, AvaticaServerConfiguration avaticaServerConfiguration) {
    // Always try to enable the "provided" ServerCustomizers. The expectation is that the Factory implementation
    // will have toggles for each provided customizer, rather than a global toggle to enable customizers.
    List<ServerCustomizer<Server>> customizers = createServerCustomizers(conf, avaticaServerConfiguration);
    if (customizers != null && !customizers.isEmpty()) {
      builder.withServerCustomizers(customizers, Server.class);
    }
  }

  //@VisibleForTesting
  public AvaticaServerConfiguration enableCustomAuth(HttpServer.Builder<Server> builder,
                                                     Configuration conf, UserGroupInformation ugi) {
    AvaticaServerConfiguration avaticaServerConfiguration = createAvaticaServerConfig(conf, ugi);
    builder.withCustomAuthentication(avaticaServerConfiguration);
    return avaticaServerConfiguration;
  }

  private static final RemoteUserExtractorFactory DEFAULT_USER_EXTRACTOR =
    new RemoteUserExtractorFactory.RemoteUserExtractorFactoryImpl();

  private static final ServerCustomizersFactory DEFAULT_SERVER_CUSTOMIZERS =
    new ServerCustomizersFactory.ServerCustomizersFactoryImpl();

  private static final AvaticaServerConfigurationFactory DEFAULT_SERVER_CONFIG =
    new AvaticaServerConfigurationFactory.AvaticaServerConfigurationFactoryImpl();

  //@VisibleForTesting
  RemoteUserExtractor createRemoteUserExtractor(Configuration conf) {
    RemoteUserExtractorFactory factory =
        InstanceResolver.getSingleton(RemoteUserExtractorFactory.class, DEFAULT_USER_EXTRACTOR);
    return factory.createRemoteUserExtractor(conf);
  }

  //@VisibleForTesting
  List<ServerCustomizer<Server>> createServerCustomizers(Configuration conf, AvaticaServerConfiguration avaticaServerConfiguration) {
    ServerCustomizersFactory factory =
      InstanceResolver.getSingleton(ServerCustomizersFactory.class, DEFAULT_SERVER_CUSTOMIZERS);
    return factory.createServerCustomizers(conf, avaticaServerConfiguration);
  }

  //@VisibleForTesting
  AvaticaServerConfiguration createAvaticaServerConfig(Configuration conf, UserGroupInformation ugi) {
    AvaticaServerConfigurationFactory factory =
            InstanceResolver.getSingleton(AvaticaServerConfigurationFactory.class, DEFAULT_SERVER_CONFIG);
    return factory.getAvaticaServerConfiguration(conf, ugi);
  }

  /**
   * Use the correctly way to extract end user.
   */
  static class PhoenixRemoteUserExtractor implements RemoteUserExtractor{
    private final HttpQueryStringParameterRemoteUserExtractor paramRemoteUserExtractor;
    private final HttpRequestRemoteUserExtractor requestRemoteUserExtractor;
    private final String userExtractParam;

    public PhoenixRemoteUserExtractor(Configuration conf) {
      this.requestRemoteUserExtractor = new HttpRequestRemoteUserExtractor();
      this.userExtractParam = conf.get(QueryServerProperties.QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM,
              QueryServerOptions.DEFAULT_QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM);
      this.paramRemoteUserExtractor = new HttpQueryStringParameterRemoteUserExtractor(userExtractParam);
    }

    @Override
    public String extract(HttpServletRequest request) throws RemoteUserExtractionException {
      if (request.getParameter(userExtractParam) != null) {
        String extractedUser = paramRemoteUserExtractor.extract(request);
        UserGroupInformation ugi =
                UserGroupInformation
                        .createRemoteUser(stripHostNameFromPrincipal(request.getRemoteUser()));
        UserGroupInformation proxyUser = UserGroupInformation.createProxyUser(extractedUser, ugi);

        // Check if this user is allowed to be impersonated.
        // Will throw AuthorizationException if the impersonation as this user is not allowed
        try {
          ProxyUsers.authorize(proxyUser, request.getRemoteAddr());
          return extractedUser;
        } catch (AuthorizationException e) {
          throw new RemoteUserExtractionException(e.getMessage(), e);
        }
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("The parameter (" + userExtractParam + ") used to extract the remote user doesn't exist in the request.");
        }
        return requestRemoteUserExtractor.extract(request);
      }

    }
  }

  /**
   * Callback to run the Avatica server action as the remote (proxy) user instead of the server.
   */
  public static class PhoenixDoAsCallback implements DoAsRemoteUserCallback {
    private final UserGroupInformation serverUgi;
    private final SimpleLRUCache<String,UserGroupInformation> ugiCache;

    public PhoenixDoAsCallback(UserGroupInformation serverUgi, Configuration conf) {
      this.serverUgi = Objects.requireNonNull(serverUgi);
      this.ugiCache = new SimpleLRUCache<String,UserGroupInformation>(
              conf.getLong(QueryServerProperties.QUERY_SERVER_UGI_CACHE_MAX_SIZE,
                  QueryServerOptions.DEFAULT_QUERY_SERVER_UGI_CACHE_MAX_SIZE),
              conf.getInt(QueryServerProperties.QUERY_SERVER_UGI_CACHE_CONCURRENCY,
                  QueryServerOptions.DEFAULT_QUERY_SERVER_UGI_CACHE_CONCURRENCY));
    }

    @Override
    public <T> T doAsRemoteUser(String remoteUserName, String remoteAddress,
        final Callable<T> action) throws Exception {
      // We are guaranteed by Avatica that the `remoteUserName` is properly authenticated by the
      // time this method is called. We don't have to verify the wire credentials, we can assume the
      // user provided valid credentials for who it claimed it was.

      // Proxy this user on top of the server's user (the real user). Get a cached instance, the
      // LoadingCache will create a new instance for us if one isn't cached.
      UserGroupInformation proxyUser = createProxyUser(stripHostNameFromPrincipal(remoteUserName));

      // Execute the actual call as this proxy user
      return proxyUser.doAs(new PrivilegedExceptionAction<T>() {
        @Override
        public T run() throws Exception {
          return action.call();
        }
      });
    }

      //@VisibleForTesting
      UserGroupInformation createProxyUser(String remoteUserName) throws ExecutionException {
          // PHOENIX-3164 UGI's hashCode and equals methods rely on reference checks, not
          // value-based checks. We need to make sure we return the same UGI instance for a remote
          // user, otherwise downstream code in Phoenix and HBase may not treat two of the same
          // calls from one user as equivalent.
          return ugiCache.computeIfAbsent(remoteUserName, f -> UserGroupInformation.createProxyUser(f, serverUgi));
      }

      //@VisibleForTesting
      SimpleLRUCache<String,UserGroupInformation> getCache() {
          return ugiCache;
      }
  }

  /**
   * The new Jetty kerberos implementation that we use strips the realm from the principal, which
   * and Hadoop cannot process that.
   * This strips the hostname part as well, so that only the username remains.

   * @param remoteUserName the principal as received from Jetty
   * @return the principal without the hostname part
   */
  //TODO We are probably compensating for a Jetty a bug, which should really be fixed in Jetty
  // See PHOENIX-6913
  private static String stripHostNameFromPrincipal(String remoteUserName) {
      // realm got removed from remoteUserName in CALCITE-4152
      // so we remove the instance name to avoid geting KerberosName$NoMatchingRule exception
      int atSignIndex = remoteUserName.indexOf('@');
      int separatorIndex = remoteUserName.indexOf('/');
      if (atSignIndex == -1 && separatorIndex > 0) {
        remoteUserName = remoteUserName.substring(0, separatorIndex);
      }
      return remoteUserName;
  }

  public static void main(String[] argv) throws Exception {
    int ret = ToolRunner.run(HBaseConfiguration.create(), new QueryServer(), argv);
    System.exit(ret);
  }
}
