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

import org.apache.calcite.avatica.server.RemoteUserExtractor;
import org.apache.hadoop.conf.Configuration;

/**
 * Creates remote user extractors.
 */
public interface RemoteUserExtractorFactory {

  RemoteUserExtractor createRemoteUserExtractor(Configuration conf);

  class RemoteUserExtractorFactoryImpl implements RemoteUserExtractorFactory {
    @Override
    public RemoteUserExtractor createRemoteUserExtractor(Configuration conf) {
      return new QueryServer.PhoenixRemoteUserExtractor(conf);
    }
  }
}
