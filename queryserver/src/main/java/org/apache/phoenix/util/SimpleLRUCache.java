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
package org.apache.phoenix.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Minimal Cache implementation based on ConcurrentHashMap.
 *
 * The maxSize logic will only work if all access is via the the computeIfAbsent() method.
 *
 */
public class SimpleLRUCache <K extends Comparable, V> extends ConcurrentHashMap<K,V> {

    protected static final Logger LOG = LoggerFactory.getLogger(SimpleLRUCache.class);

    int maxSize;
    int triggerSize;

    private ConcurrentHashMap<K, AtomicLong> accessed =
            new ConcurrentHashMap<>();

    public SimpleLRUCache (long maxSize, int concurrencyLevel) {
        super((int)(maxSize * 1.1), (float)0.75, concurrencyLevel);
        this.maxSize = (int)maxSize;
        this.triggerSize = (int)(maxSize * 1.1)+1 ;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
        V value = super.computeIfAbsent(key, mappingFunction);
        if (value != null) {
            accessed.put(key, new AtomicLong(System.currentTimeMillis()));
            if (this.size() > triggerSize) {
                evict();
            }
        }
        return value;
    }

    private void evict() {
        synchronized(this) {
            int currentSize = this.size();
            if (currentSize <= triggerSize) {
                return;
            }
            LOG.warn("UGI Cache capacity exceeded, you may want to increase its size");
            TreeSet<Entry<K, AtomicLong>> sortedByLRU = new TreeSet<>(
                    new Comparator<Entry<K, AtomicLong>>() {
                        @Override
                        public int compare(Entry<K, AtomicLong> o1, Entry<K, AtomicLong> o2) {
                            int keyResult =
                                    Long.compare(o2.getValue().get(), o1.getValue().get());
                            if(keyResult == 0) {
                                return o2.getKey().compareTo(o1.getKey());
                            } else {
                                return keyResult;
                            }
                        }
                    });
            sortedByLRU.addAll(accessed.entrySet());
            Entry<Object, AtomicLong>[] toRetain =
                    Arrays.copyOfRange(sortedByLRU.toArray(new Entry[currentSize]), 0, maxSize);
            java.util.List<Object> retainList =
                    Arrays.stream(toRetain).map( f -> f.getKey()).collect(Collectors.toList());
            this.keySet().retainAll(retainList);
            accessed.keySet().retainAll(this.keySet());
        }
    }

}
