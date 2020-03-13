/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import sqlline.SqlLine;

/**
 * Utility class which automatically performs a Kerberos login and then launches sqlline. Tries to
 * make a pre-populated ticket cache (via kinit before launching) transparently work.
 */
public class SqllineWrapper {

  public static String getUrl(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      args[i] = arg;
      if (arg.equals("-u") && args.length > i+1) {
        return args[i+1];
      }
    }
    return null;
  }

  public static void main(String[] args) throws Exception {
    String url = getUrl(args);

    if(url.contains(";authentication=SPNEGO") && !url.contains(";principal=")) {
      try {
        Subject subject = KerberosLoginFromTicketCache.login();
        System.out.println("Kerberos login from ticket cache successful");
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
          @Override
          public Void run() throws Exception {
            SqlLine.main(args);
            return null;
          }
        });
        return;
      } catch (LoginException e) {
        System.out.print("Kerberos login from ticket cache not successful");
        e.printStackTrace();
      }
    }

    SqlLine.main(args);
  }

}
