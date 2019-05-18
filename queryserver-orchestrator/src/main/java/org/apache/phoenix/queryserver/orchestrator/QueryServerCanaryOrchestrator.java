/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.queryserver.orchestrator;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

import org.apache.curator.utils.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/*
    This is a main class to which initiates the relevant clients
    to run orchestrator for queryserver canary
 */

public class QueryServerCanaryOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryServerCanaryOrchestrator.class);

    private static String ZK_PATH = "/pqs/leader";

    private static Namespace parseArgs(String[] args) {

        ArgumentParser parser = ArgumentParsers.newFor("PQS Canary Orchestrator").build()
                .description("PQS Canary Orchestrator");

        parser.addArgument("--zkurl", "-zk").type(String.class).help("URL for Zookeeper");

        parser.addArgument("--zkpath", "-zkp").type(String.class).nargs("?").setDefault(ZK_PATH)
                .help("ZKNode path default: " + ZK_PATH);

        parser.addArgument("--hostname", "-hn").type(String.class).nargs("?").setDefault("localhost").help("Hostname on "
                + "which PQS is running.");

        parser.addArgument("--port", "-p").type(String.class).nargs("?").setDefault("8765").help("Port on which PQS " +
                "is running.");

        parser.addArgument("--constring", "-cs").type(String.class).nargs("?").help("Pass an " +
                "explicit " + "connection String to connect to PQS. default: null");

        parser.addArgument("--timeout", "-t").type(String.class).nargs("?").setDefault("120")
                .help("Maximum time for which the app should run before returning error. default: 120 sec");

        parser.addArgument("--testtable", "-tt").type(String.class).nargs("?").setDefault
                ("PQSTEST").help("Custom name for the test table. default: PQSTEST");

        parser.addArgument("--testschema", "-ts").type(String.class).nargs("?").setDefault
                ("TEST").help("Custom name for the test table. default: TEST");

        parser.addArgument("--logsinkclass", "-lsc").type(String.class).nargs("?").setDefault
                ("org.apache.phoenix.tool.PhoenixCanaryTool$StdOutSink")
                .help("Path to a Custom implementation for log sink class. default: stdout");

        parser.addArgument("--interval", "-in").type(String.class).nargs("?").setDefault("900")
                .help("Time interval between 2 consecutive test suite runs");


        Namespace res = null;
        try {
            res = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
        return res;
    }

    public static Map<String, String> getArgs(Namespace cArgs) {

        Map<String, String> params = new HashMap<>();

        for (Map.Entry<String, Object> entry : cArgs.getAttrs().entrySet()) {
            params.put(entry.getKey(), (String) entry.getValue());
        }
        return params;
    }

    public static void main(String[] args) {

        LOGGER.info("Starting PQS Canary Orchestrator...");
        ToolWrapper tool = new ToolWrapper();
        try {
            Namespace cArgs = parseArgs(args);
            if (cArgs == null) {
                throw new RuntimeException("Argument parsing failed");
            }

            Map<String, String> params = getArgs(cArgs);
            ZK_PATH = params.get("zkpath");

            CuratorFramework curatorClient = CuratorFrameworkFactory.newClient(params.get("zkurl"), new
                    BoundedExponentialBackoffRetry(1000, 60000, 50000));
            curatorClient.start();

            TestExecutorClient testExecutorClient = new TestExecutorClient(curatorClient, ZK_PATH, params, tool);

            try {
                testExecutorClient.start();
                while (!testExecutorClient.isDone()) {
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                LOGGER.error("The Main thread was interrupted", ex);
            } finally {
                LOGGER.info("Shutdown Hook for PQS Canary Orchestrator running...");
                CloseableUtils.closeQuietly(testExecutorClient);
                CloseableUtils.closeQuietly(curatorClient);
                LOGGER.info("Closed Curator Client");
            }

        } catch (Exception e) {
            LOGGER.error("Error in PQS Canary Orchestrator. ", e);
            throw new RuntimeException(e);
        }
        LOGGER.info("Exiting PQS Canary Orchestrator...");
    }

}
