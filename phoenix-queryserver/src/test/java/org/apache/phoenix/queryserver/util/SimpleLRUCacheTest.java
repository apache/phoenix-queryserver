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
package org.apache.phoenix.queryserver.util;

import static org.junit.Assert.assertTrue;

import org.apache.phoenix.util.SimpleLRUCache;
import org.junit.Test;

public class SimpleLRUCacheTest {

    @Test
    public void testCache() throws InterruptedException {
        SimpleLRUCache<String, String> cache = new SimpleLRUCache<>(10, 1);

        String zero = access(cache, "0");
        //Make sure we actually cache objects
        assertTrue(zero  == access(cache, "0"));

        for(int c=1; c<9; c++) {
            access(cache, Integer.toString(c));
            Thread.sleep(5);
        }

        //Access these to make sure that they don't get evicted.
        String one = access(cache, "1");
        String two = access(cache, "2");

        for(int c=10; c<13; c++) {
            access(cache, Integer.toString(c));
            Thread.sleep(5);
        }

        assertTrue(one == access(cache, one));
        assertTrue(two == access(cache, two));

        assertTrue(cache.size() <= 12);
    }

    private String access(SimpleLRUCache<String, String> cache, String key) {
        return cache.computeIfAbsent(key, k -> new String(key));
    }
}
