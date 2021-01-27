<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

![logo](https://phoenix.apache.org/images/phoenix-logo-small.png)

<b>[Apache Phoenix](http://phoenix.apache.org/)</b> enables OLTP and operational analytics in Hadoop for low latency applications. Visit the Apache Phoenix website <b>[here](http://phoenix.apache.org/)</b>. This is the repo for the Phoenix Query Server (PQS).

Copyright ©2020 [Apache Software Foundation](http://www.apache.org/). All Rights Reserved.

## Building

This repository will build a tarball which is capable of running the Phoenix Query Server.

By default, this tarball does not contain a Phoenix client jar as it is meant to be agnostic
of Phoenix version (one PQS release can be used against any Phoenix version). Today, PQS builds against
the Phoenix 4.15.0-HBase-1.4 release.

Note that the resulting Query Server binaries are not tied to any Phoenix, Hbase or Hadoop versions,
and will work with all recent Phoenix versions.

```
$ mvn clean package
```

### Bundling a Phoenix Client

To build a release of PQS which packages a specific version of Phoenix, specify the `package-phoenix-client` system property
and specify the `phoenix.version` system property to indicate a specific Phoenix version, as well as
the `phoenix.client.artifactid` to choose the phoenix-client HBase variant for 4.16+ / 5.1+.

```
$ mvn clean package -Dpackage.phoenix.client -Dphoenix.version=5.1.0-SNAPSHOT -Dphoenix.client.artifactid=phoenix-client-hbase-2.4
```

### Running integration tests

`mvn package` will run the unit tests while building, but it will not run the integration test suite.

The IT suite is run when executing `mvn install` or `mvn verify`. The Phoenix client artifact specified
with `phoenix.version` and `phoenix.client.artifactid` is used for running the integration tests.

When specifying `phoenix.version` and `phoenix.client.artifactid`, also specify the HBase version to
be used for integration testing by activating the corresponding `hbase<minor.major>` profile.

When using a Phoenix 5.1+ version, activate the `hbase-2.x` profile in addition to the
profile for the minor 2.x version.

**NOTE** that the integration tests cannot be currently run with Phoenix 4.16+ or 5.1+, so the
examples below will fail. See https://issues.apache.org/jira/browse/PHOENIX-6324

```
$ mvn clean verify -Dpackage.phoenix.client -Dphoenix.version=4.16.0-SNAPSHOT -Dphoenix.client.artifactid=phoenix-client-hbase-1.3 -Phbase-1.3
```
```
$ mvn clean install -Dpackage.phoenix.client -Dphoenix.version=5.1.0-SNAPSHOT -Dphoenix.client.artifactid=phoenix-client-hbase-2.1 -Phbase-2.1 -Phbase-2.x
```

### Running project reports

Phoenix-queryserver currently supports generating the standard set of Maven Project Info Reports,
as well as Spotbugs, Apache Creadur RAT, OWASP Dependency-Check, and Jacoco Code Coverage reports.

To run all available reports
`$ mvn clean verify site -Dspotbugs.site`

To run OWASP, RAT and Spotbugs, but not Jacoco
`$ mvn clean compile test-compile site -Dspotbugs.site`

The reports are accessible via `target/site/index.html`, under the main project,
as well as each of the subprojects. (not every project has all reports)
