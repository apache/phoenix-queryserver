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
package org.apache.phoenix.queryserver.server.admin;


import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.apache.phoenix.mapreduce.index.IndexScrutinyTool;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import org.apache.phoenix.mapreduce.index.IndexTool;
/**
 *
 */
public class AdminServerHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("POST".equalsIgnoreCase(baseRequest.getMethod())) {
            response.setContentType("application/json");
            try {
                AdminCommandRequest adminCommand = parseRequest(baseRequest.getInputStream());
                response.setStatus(HttpServletResponse.SC_OK);
                Tool tool = null;
                switch (adminCommand.getCommand()) {
                    case IndexTool:
                        tool = new IndexTool();
                        break;
                    case IndexScrutiny:
                        tool = new IndexScrutinyTool();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported tool: " + adminCommand.getCommand());
                }
               int runStatus = ToolRunner.run(tool, adminCommand.getParametersAsInputArgs());
            } catch (Exception e) {

            }
            response.getWriter().println("<h1>Hello OneHandler</h1>");
            response.flushBuffer();
        }

    }

    private AdminCommandRequest parseRequest(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, AdminCommandRequest.class);
    }

}
