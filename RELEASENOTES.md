
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
# PHOENIX  queryserver-6.0.0 Release Notes

These release notes cover new developer and user-facing incompatibilities, important issues, features, and major improvements.


---

* [PHOENIX-6488](https://issues.apache.org/jira/browse/PHOENIX-6488) | *Major* | **Bump Avatica version to 1.18.0 in queryserver**

Avatica version  has been update to 1.18.0


---

* [PHOENIX-6473](https://issues.apache.org/jira/browse/PHOENIX-6473) | *Major* | **Add Hadoop JMXServlet as /jmx endpoint**

New HTTP endpoint is available at \<pqs\_url\>/jmx to expose all read-only JMX values in JSON format.


---

* [PHOENIX-5446](https://issues.apache.org/jira/browse/PHOENIX-5446) | *Major* | **Support Protobuf shaded clients (thin + thick)**

**WARNING: No release note provided for this change.**


---

* [PHOENIX-5772](https://issues.apache.org/jira/browse/PHOENIX-5772) | *Major* | **Streamline the kerberos logic in thin client java code**

The java thin client library has been refactored. It no longer includes Hadoop libraries, and uses the Java SE Kerberos implementation directly.

The sqlline library has also been removed from the thin client JAR.

The standalone sqlline JAR is now included in the lib/ directory, and is now added to the classpath by the sqlline-thin.py script.

Some default parameters that were picked up from hbase-site.xml by the java thin client are now also handled by sqlline-thin.py



