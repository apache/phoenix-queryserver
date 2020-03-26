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
package org.apache.phoenix.end2end;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hbase.http.ssl.KeyStoreTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsUtil {

    protected static final String KEYSTORE_PASSWORD = "avaticasecret";
    protected static final String TRUSTSTORE_PASSWORD = "avaticasecret";

    protected static final String TARGET_DIR_NAME = System.getProperty("target.dir", "target");
    protected static final File TARGET_DIR =
            new File(System.getProperty("user.dir"), TARGET_DIR_NAME);
    protected static final File KEYSTORE = new File(TARGET_DIR, "avatica-test-ks.jks");
    protected static final File TRUSTSTORE = new File(TARGET_DIR, "avatica-test-ts.jks");

    private static final Logger LOG = LoggerFactory.getLogger(QueryServerBasicsIT.class);

    public static File getTrustStoreFile() {
        return TRUSTSTORE;
    }

    public static File getKeyStoreFile() {
        return KEYSTORE;
    }

    public static String getTrustStorePassword() {
        return TRUSTSTORE_PASSWORD;
    }

    public static String getKeyStorePassword() {
        return KEYSTORE_PASSWORD;
    }

    static {
        try {
            setupTls();
        } catch (Exception e) {
            LOG.error("could not set upt TLS for HTTPS tests", e);
        }
    }

    /**
     * This is simplified from org.apache.hadoop.hbase.http.ssl.KeyStoreTestUtil.setupSSLConfig()
     * Performs setup of SSL configuration in preparation for testing an SSLFactory. This includes
     * keys, certs, keystores, truststores.
     */
    public static void setupTls() throws Exception {

        try {
            KEYSTORE.delete();
        } catch (Exception e) {
            // may not exist
        }

        try {
            TRUSTSTORE.delete();
        } catch (Exception e) {
            // may not exist
        }

        KeyPair sKP = KeyStoreTestUtil.generateKeyPair("RSA");
        X509Certificate sCert =
                KeyStoreTestUtil.generateCertificate("CN=localhost, O=server", sKP, 30,
                    "SHA1withRSA");
        KeyStoreTestUtil.createKeyStore(KEYSTORE.getCanonicalPath(), KEYSTORE_PASSWORD, "server",
            sKP.getPrivate(), sCert);

        Map<String, X509Certificate> certs = new HashMap<String, X509Certificate>();
        certs.put("server", sCert);

        KeyStoreTestUtil.createTrustStore(TRUSTSTORE.getCanonicalPath(), TRUSTSTORE_PASSWORD,
            certs);
    }

}
