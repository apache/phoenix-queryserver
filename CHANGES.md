
<!---
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
-->
# PHOENIX Changelog

## Release queryserver-6.0.0 - Unreleased (as of 2021-07-16)



### IMPORTANT ISSUES:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-5446](https://issues.apache.org/jira/browse/PHOENIX-5446) | Support Protobuf shaded clients (thin + thick) |  Major | . |


### NEW FEATURES:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-5938](https://issues.apache.org/jira/browse/PHOENIX-5938) | Support impersonation in the python driver |  Major | python, queryserver |
| [PHOENIX-5880](https://issues.apache.org/jira/browse/PHOENIX-5880) | Add SQLAchemy support to python driver |  Major | python, queryserver |
| [PHOENIX-5642](https://issues.apache.org/jira/browse/PHOENIX-5642) | Add HTTPS support to Phoenix Query Server and thin client |  Major | queryserver |


### IMPROVEMENTS:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-6508](https://issues.apache.org/jira/browse/PHOENIX-6508) | add -bin suffix to queryserver binary assembly name |  Trivial | queryserver |
| [PHOENIX-6488](https://issues.apache.org/jira/browse/PHOENIX-6488) | Bump Avatica version to 1.18.0 in queryserver |  Major | queryserver |
| [PHOENIX-6473](https://issues.apache.org/jira/browse/PHOENIX-6473) | Add Hadoop JMXServlet as /jmx endpoint |  Major | queryserver |
| [PHOENIX-6398](https://issues.apache.org/jira/browse/PHOENIX-6398) | Returns uniform SQL dialect in calcite for the PQS |  Major | queryserver |
| [PHOENIX-5869](https://issues.apache.org/jira/browse/PHOENIX-5869) | Use symlinks to reduce size of phoenix queryserver assembly |  Major | queryserver |
| [PHOENIX-5829](https://issues.apache.org/jira/browse/PHOENIX-5829) | Make it possible to build/test queryserver against all supported versions |  Major | queryserver |
| [PHOENIX-6006](https://issues.apache.org/jira/browse/PHOENIX-6006) | Bump queryserver version to 6.0 |  Major | queryserver |
| [PHOENIX-5999](https://issues.apache.org/jira/browse/PHOENIX-5999) | Have executemany leverage ExecuteBatchRequest |  Major | python |
| [PHOENIX-6007](https://issues.apache.org/jira/browse/PHOENIX-6007) | PhoenixDB error handling improvements |  Major | queryserver |
| [PHOENIX-5778](https://issues.apache.org/jira/browse/PHOENIX-5778) | Remove the dependency of KeyStoreTestUtil |  Major | queryserver |
| [PHOENIX-5964](https://issues.apache.org/jira/browse/PHOENIX-5964) | Rename queryserver subprojects |  Major | queryserver |
| [PHOENIX-5907](https://issues.apache.org/jira/browse/PHOENIX-5907) | Remove unused part from phoenix\_utils.py |  Major | queryserver |
| [PHOENIX-5904](https://issues.apache.org/jira/browse/PHOENIX-5904) | Add log if the configed kerberos principal login failed |  Minor | queryserver |
| [PHOENIX-5826](https://issues.apache.org/jira/browse/PHOENIX-5826) | Remove guava from queryserver |  Major | queryserver |
| [PHOENIX-5844](https://issues.apache.org/jira/browse/PHOENIX-5844) | Feature parity for the python client |  Major | python |
| [PHOENIX-5827](https://issues.apache.org/jira/browse/PHOENIX-5827) | Let PQS act as a maven repo |  Major | queryserver |
| [PHOENIX-4112](https://issues.apache.org/jira/browse/PHOENIX-4112) | Allow JDBC url-based Kerberos credentials via sqlline-thin.py |  Major | queryserver |
| [PHOENIX-5814](https://issues.apache.org/jira/browse/PHOENIX-5814) | disable trimStackTrace |  Major | connectors, core, omid, queryserver, tephra |
| [PHOENIX-5777](https://issues.apache.org/jira/browse/PHOENIX-5777) | Unify the queryserver config keys to use QueryServerProperties |  Major | queryserver |
| [PHOENIX-5702](https://issues.apache.org/jira/browse/PHOENIX-5702) | Add https support to sqlline-thin script |  Major | queryserver |
| [PHOENIX-5454](https://issues.apache.org/jira/browse/PHOENIX-5454) | Phoenix scripts start foreground java processes as child processes |  Minor | core, queryserver |
| [PHOENIX-5459](https://issues.apache.org/jira/browse/PHOENIX-5459) | Enable running the test suite with JDK11 |  Major | . |
| [PHOENIX-5393](https://issues.apache.org/jira/browse/PHOENIX-5393) | Perform \_HOST principal expansion for SPENGO QueryServer principal |  Blocker | queryserver |


### BUG FIXES:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-6512](https://issues.apache.org/jira/browse/PHOENIX-6512) | Fix PQS Apache RAT check problems |  Major | queryserver |
| [PHOENIX-6407](https://issues.apache.org/jira/browse/PHOENIX-6407) | phoenixdb for Python silently ignores placeholders \< placeholder arguments |  Minor | python |
| [PHOENIX-6461](https://issues.apache.org/jira/browse/PHOENIX-6461) | sqlline-thin does not include slf4j logging backend |  Major | queryserver |
| [PHOENIX-6324](https://issues.apache.org/jira/browse/PHOENIX-6324) | Queryserver IT suite incompatible with phoenix 5.1+ / 4.16+ shading |  Major | queryserver |
| [PHOENIX-6489](https://issues.apache.org/jira/browse/PHOENIX-6489) | Adopt PQS ITs to use Phoenix 5.1 |  Major | queryserver |
| [PHOENIX-6463](https://issues.apache.org/jira/browse/PHOENIX-6463) | PQS jar hosting doesn't include pom's |  Major | queryserver |
| [PHOENIX-6414](https://issues.apache.org/jira/browse/PHOENIX-6414) | Access to Phoenix from Python using SPNEGO |  Major | python, queryserver |
| [PHOENIX-6177](https://issues.apache.org/jira/browse/PHOENIX-6177) | Queryserver Avatica and Jetty versions are incompatible |  Blocker | queryserver |
| [PHOENIX-6325](https://issues.apache.org/jira/browse/PHOENIX-6325) | Adapt queryserver build to the new phoenix-client artifactIds |  Blocker | queryserver |
| [PHOENIX-6294](https://issues.apache.org/jira/browse/PHOENIX-6294) | javax.servlet relocation added by PHOENIX-6151 breaks PQS |  Blocker | queryserver |
| [PHOENIX-6238](https://issues.apache.org/jira/browse/PHOENIX-6238) | Fix missing executable permission because of MASSEMBLY-941 |  Major | connectors, queryserver |
| [PHOENIX-6162](https://issues.apache.org/jira/browse/PHOENIX-6162) | Apply PHOENIX-5594 to the phoenix-queryserver repo |  Major | queryserver |
| [PHOENIX-5994](https://issues.apache.org/jira/browse/PHOENIX-5994) | SqlAlchemy schema filtering incorrect semantics |  Major | queryserver |
| [PHOENIX-5901](https://issues.apache.org/jira/browse/PHOENIX-5901) | Add LICENSE and NOTICE files to phoenix-queryserver |  Blocker | queryserver |
| [PHOENIX-5959](https://issues.apache.org/jira/browse/PHOENIX-5959) | python scripts not working for phoenix-queryserver |  Major | queryserver |
| [PHOENIX-5936](https://issues.apache.org/jira/browse/PHOENIX-5936) | sqlAlchemy get\_columns KeyError: None |  Major | queryserver |
| [PHOENIX-5831](https://issues.apache.org/jira/browse/PHOENIX-5831) | Make Phoenix queryserver scripts work with Python 3 |  Critical | queryserver |
| [PHOENIX-5852](https://issues.apache.org/jira/browse/PHOENIX-5852) | The zkConnectionString in LoadBalance is incorrect |  Major | queryserver |
| [PHOENIX-5879](https://issues.apache.org/jira/browse/PHOENIX-5879) | Fix recently introduced python 2.7 incompatibilities and flake8 warning |  Major | queryserver |
| [PHOENIX-5873](https://issues.apache.org/jira/browse/PHOENIX-5873) | Fix loadbalancer packaging problem for queryserver scripts |  Major | queryserver |
| [PHOENIX-5830](https://issues.apache.org/jira/browse/PHOENIX-5830) | Fix python printing in query-server |  Major | queryserver |
| [PHOENIX-5761](https://issues.apache.org/jira/browse/PHOENIX-5761) | sqlline-thin kerberos logic too aggressive |  Major | queryserver |
| [PHOENIX-5741](https://issues.apache.org/jira/browse/PHOENIX-5741) | Spurious protobuf version property in queryserver-client pom |  Trivial | queryserver |
| [PHOENIX-5670](https://issues.apache.org/jira/browse/PHOENIX-5670) | Add the neccesary Synchronisation to the tests in the PQS repo |  Major | queryserver |
| [PHOENIX-5234](https://issues.apache.org/jira/browse/PHOENIX-5234) | Create patch scripts for phoenix-connectors and phoenix-queryserver projects |  Major | connectors, queryserver |
| [PHOENIX-5421](https://issues.apache.org/jira/browse/PHOENIX-5421) | Phoenix Query server tests race condition issue on creating keytab folder |  Blocker | queryserver |
| [PHOENIX-5394](https://issues.apache.org/jira/browse/PHOENIX-5394) | Integration tests not running for phoenix-queryserver, general build cruft. |  Blocker | queryserver |
| [PHOENIX-5221](https://issues.apache.org/jira/browse/PHOENIX-5221) | Phoenix Kerberos Integration tests failure on Redhat Linux |  Blocker | queryserver |


### SUB-TASKS:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-5846](https://issues.apache.org/jira/browse/PHOENIX-5846) | Let the python client parse options from the JDBC URL |  Major | python, queryserver |
| [PHOENIX-5859](https://issues.apache.org/jira/browse/PHOENIX-5859) | Add Array support to the python driver |  Major | python, queryserver |
| [PHOENIX-5858](https://issues.apache.org/jira/browse/PHOENIX-5858) | Add commit/rollback support to the python client |  Major | python, queryserver |
| [PHOENIX-5857](https://issues.apache.org/jira/browse/PHOENIX-5857) | Get the python test suite working |  Major | python, queryserver |
| [PHOENIX-5848](https://issues.apache.org/jira/browse/PHOENIX-5848) | Add DIGEST auth support to the python client |  Major | python, queryserver |
| [PHOENIX-5847](https://issues.apache.org/jira/browse/PHOENIX-5847) | Add BASIC auth support to the python client |  Major | python, queryserver |
| [PHOENIX-5856](https://issues.apache.org/jira/browse/PHOENIX-5856) | Switch the python driver to requests\_gssapi |  Major | python, queryserver |
| [PHOENIX-5845](https://issues.apache.org/jira/browse/PHOENIX-5845) | Add HTTPS support to the python client |  Major | python, queryserver |


### OTHER:

| JIRA | Summary | Priority | Component |
|:---- |:---- | :--- |:---- |
| [PHOENIX-5759](https://issues.apache.org/jira/browse/PHOENIX-5759) | Reduce thin client JAR size / classpath noise |  Major | queryserver |
| [PHOENIX-6065](https://issues.apache.org/jira/browse/PHOENIX-6065) | Add OWASP dependency check, and update the flagged direct dependencies |  Major | connectors, core, queryserver |
| [PHOENIX-5939](https://issues.apache.org/jira/browse/PHOENIX-5939) | Publish PhoenixDB to PyPI |  Major | python, queryserver |
| [PHOENIX-6060](https://issues.apache.org/jira/browse/PHOENIX-6060) | Create release script for python-phoenixdb ASF releases |  Major | python, queryserver |
| [PHOENIX-5771](https://issues.apache.org/jira/browse/PHOENIX-5771) | Make standalone queryserver assembly useful again |  Major | queryserver |
| [PHOENIX-5824](https://issues.apache.org/jira/browse/PHOENIX-5824) | Add dependency:analyze to queryserver build |  Major | queryserver |
| [PHOENIX-5772](https://issues.apache.org/jira/browse/PHOENIX-5772) | Streamline the kerberos logic in thin client java code |  Major | queryserver |
| [PHOENIX-5680](https://issues.apache.org/jira/browse/PHOENIX-5680) | remove psql.py from phoenix-queryserver |  Minor | queryserver |



