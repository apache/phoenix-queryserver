/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.phoenix.queryserver.orchestrator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
    This is a class which selects the leader among all the orchestrator hosts using curator framework
    and appropriately calls ToolWrapper.
 */

public class TestExecutorClient extends LeaderSelectorListenerAdapter implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestExecutorClient.class);
    private final LeaderSelector leaderSelector;

    private Map<String, String> params;
    private ToolWrapper toolWrapper;
    private Boolean isRunning = false;

    public TestExecutorClient(CuratorFramework client, String path, Map<String, String> params, ToolWrapper tool) {

        // create a leader selector using the given path for management
        // all participants in a given leader selection must use the same path
        // ExampleClient here is also a LeaderSelectorListener but this isn't required
        this.leaderSelector = new LeaderSelector(client, path, this);

        // for most cases you will want your instance to requeue when it relinquishes leadership
        this.leaderSelector.autoRequeue();
        this.params = params;
        this.toolWrapper = tool;
    }

    public void close() {
        leaderSelector.close();
    }

    public void start() {
        leaderSelector.start();
        isRunning = true;
    }

    @Override
    public void takeLeadership(CuratorFramework client) throws Exception {
        // we are now the leader. This method should not return until we want to relinquish
        // leadership
        LOGGER.info("Took leadership.");

        while (true) {
            LOGGER.info("Starting test case suite execution.");
            executeQueryServerCanaryTool();

            LOGGER.info("Test suite execution completed. Waiting for " + params.get("interval")
                    + " secs before executing next run.");

            TimeUnit.SECONDS.sleep(Integer.parseInt(params.get("interval")));
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        LOGGER.info("ZK Connection State Changed to [{}]", newState.name());
        switch(newState) {
            case CONNECTED:
                LOGGER.info("The host is connected at" +System.currentTimeMillis());
                break;
            case LOST:
                LOGGER.info("The host lost connection at "+ System.currentTimeMillis());
                isRunning = false;
                throw new CancelLeadershipException();
            case READ_ONLY:
                LOGGER.info("The connection state is Read only since "+ System.currentTimeMillis());
                break;
            case RECONNECTED:
                LOGGER.info("Reconnected to the ZK Path at " +System.currentTimeMillis());
                break;
            case SUSPENDED:
                isRunning = false;
                throw new CancelLeadershipException();
            default:
        }
    }

    //@VisibleForTesting
    public void executeQueryServerCanaryTool() {
        List<String> cmd = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                cmd.add("--" + entry.getKey());
                cmd.add(entry.getValue());
            }
        }
        LOGGER.info("Test Suit execution started.");
        toolWrapper.executeMain(cmd.toArray(new String[cmd.size()]));
    }

    public boolean isDone() {
        return !isRunning;
    }
}
