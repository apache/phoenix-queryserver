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
package org.apache.phoenix.queryserver.client;

import java.io.IOException;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class KerberosLoginFromTicketCache {

    public static Subject login() throws LoginException {

        Configuration kerberosConfig = new KerberosConfiguration();
        Subject subject = new Subject();

        LoginContext lc = new LoginContext("PhoenixThinClient", subject, new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks)
                    throws IOException, UnsupportedCallbackException {
                throw new UnsupportedCallbackException(callbacks[0],
                        "Only ticket cache is supported");
            }
        }, kerberosConfig);

        lc.login();
        return subject;
    }

    private static class KerberosConfiguration extends Configuration {
        private static final String IBM_KRB5_LOGIN_MODULE =
                "com.ibm.security.auth.module.Krb5LoginModule";
        private static final String SUN_KRB5_LOGIN_MODULE =
                "com.sun.security.auth.module.Krb5LoginModule";

        private static final String JAVA_VENDOR_NAME = System.getProperty("java.vendor");
        private static final boolean IS_IBM_JAVA = JAVA_VENDOR_NAME.contains("IBM");

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            HashMap<String, Object> options = new HashMap<>();

            if(IS_IBM_JAVA) {
                //Also see https://www.ibm.com/support/pages/how-enable-strong-encryption-128-bit

                // This is inferior to the sun class, as it won't work if the kerberos and unix
                // users don't match, while that one take any principal from the cache
                options.put("principal", System.getProperty("user.name"));
                options.put("useDefaultCcache", "true");
                return new AppConfigurationEntry[] { new AppConfigurationEntry(
                    IBM_KRB5_LOGIN_MODULE, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options) };
            } else {
                options.put("useTicketCache", "true");
                options.put("doNotPrompt", "true");
                return new AppConfigurationEntry[] { new AppConfigurationEntry(
                    SUN_KRB5_LOGIN_MODULE, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options) };
            }
        }
    }

}
