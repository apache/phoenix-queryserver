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
package org.apache.phoenix.loadbalancer.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.junit.Assert;
import org.junit.Test;

public class LoadBalanceZookeeperConfImplTest {
    @Test
    public void testZkConnectString() {
        // server port is different from the port which is set by user
        testZKClusterKey("server:2183", "2182");
        // multiple servers have their own port
        testZKClusterKey("server1:2182,server2:2183,server3:2184", "2181");
        // some servers no specified port
        testZKClusterKey("server1,server2:2181,server3:2182", "2181");
        testZKClusterKey("server1:2182,server2,server3:2183", "2181");
        testZKClusterKey("server1:2182,server2:2183,server3", "2181");
        testZKClusterKey("server1:2182,server2,server3:2183", "2184");
        // no port set
        testZKClusterKey("server1,server2,server3", "");
        testZKClusterKey("server1:2182,server2,server3:2183", "");
    }

    private void testZKClusterKey(String quorum, String port) {
        final Configuration conf = HBaseConfiguration.create();
        conf.set(QueryServerProperties.ZOOKEEPER_QUORUM_ATTRIB, quorum);
        conf.set(QueryServerProperties.ZOOKEEPER_PORT_ATTRIB, port);
        final LoadBalanceZookeeperConfImpl loadBalanceZookeeperConf = new LoadBalanceZookeeperConfImpl(conf);
        String[] connectStrings = loadBalanceZookeeperConf.getZkConnectString().split(",");
        String[] quorums = quorum.split(",");
        Assert.assertTrue( connectStrings.length == quorums.length);
        for (int i = 0; i< connectStrings.length; ++i) {
            if (quorums[i].contains(":")) {
                Assert.assertEquals(quorums[i], connectStrings[i]);
            } else {
                Assert.assertEquals(quorums[i] + ":" + port, connectStrings[i]);
            }
        }
    }
}
