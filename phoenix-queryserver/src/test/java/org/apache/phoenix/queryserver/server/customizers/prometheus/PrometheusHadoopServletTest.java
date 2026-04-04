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

package org.apache.phoenix.queryserver.server.customizers.prometheus;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrometheusHadoopServletTest {
    private final PrometheusHadoopServlet servlet = new PrometheusHadoopServlet();

    //NullPointerException is expected because the MetricExportHelper doesn't have any metrics to export
    @Test(expected = NullPointerException.class)
    public void testPrometheusServletMetricsWriter() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);

        PrintWriter writer = new PrintWriter(System.out);
        when(response.getWriter()).thenReturn(writer);

        servlet.writeMetrics(response.getWriter(),false,"");
    }

    @Test
    public void testPrometheusNameConversion() {
        String metricRecordName = "TestMetricRecord";
        String metricName = "TestMetricName";

        Assert.assertEquals("test_metric_record_test_metric_name",servlet.toPrometheusName(metricRecordName,metricName));
    }
}

