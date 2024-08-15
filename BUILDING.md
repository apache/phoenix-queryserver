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
of Phoenix version (one PQS release should be usable against any Phoenix version).

However, due to an incompatible change in the relocations used in the phoenix-client JAR, you need to build
Phoenix Query Server with the `shade-javax-servlet` maven profile if you use Phoenix versions
5.1.1, 5.1.2, 5.1.3 or 4.16.x with it. (See PHOENIX-6861 for more details)
This applies whether you bundle the Phoenix client into the assembly or add it separately.
Phoenix 5.2.0 and later requires that PQS is built WITHOUT the `shade-javax-servlet` maven profile.

In order to use Phoenix Query Server, you need to copy the phoenix-client-embedded jar appropriate
for your cluster into the Queryserver root directory.

Note that the resulting Query Server binaries are not tied to any Phoenix, Hbase or Hadoop versions,
apart from the exception above.


```
$ mvn clean package -Pshade-javax-servlet
```

For other Phoenix versions build with the default settings

```
$ mvn clean package
```

### Bundling a Phoenix Client

To build a release of PQS which packages a specific version of Phoenix, specify the `package-phoenix-client` system property
and specify the `phoenix.version` system property to indicate a specific Phoenix version, as well as
the `phoenix.client.artifactid` to choose the phoenix-client HBase variant.
You need to bundle the embedded client variant, to avoid conflicts with the logging libraries.

```
$ mvn clean package -Dpackage.phoenix.client -Dphoenix.version=5.1.1 -Dphoenix.client.artifactid=phoenix-client-embedded-hbase-2.4 -Pshade-javax-servlet -pl '!phoenix-queryserver-it'
```

### Running integration tests

`mvn package` will run the unit tests while building, but it will not run the integration test suite.

The integration tests will run with the default Phoenix and HBase version.
Running the integration tests with non-default Phoenix and HBase versions may or may not work.

```
$ mvn clean verify
```


If a different Phoenix version is used for testing, then at least the *hbase.version*
and *hadoop.version* properties must be set to the versions used to build phoenix-client-embdedd,
but other changes may also be needed, or there may be un-resolvable conflicts.

```
$ mvn clean verify -Dphoenix.version=5.1.3 -Pshade-javax-servlet -Dphoenix.client.artifactid=phoenix-client-embedded-hbase-2.4 -Dhadoop.version=3.1.3 -Dhbase.version=2.4.15  -DforkCount=6'
```

(At the time of writing, the above will run, but fail because 5.1.3 does not have PHOENIX-5066
required by the failing test)

### Running project reports

Phoenix-queryserver currently supports generating the standard set of Maven Project Info Reports,
as well as Spotbugs, Apache Creadur RAT, OWASP Dependency-Check, and Jacoco Code Coverage reports.

To run all available reports
`$ mvn clean verify site -Dspotbugs.site`

To run OWASP, RAT and Spotbugs, but not Jacoco
`$ mvn clean compile test-compile site -Dspotbugs.site`

The reports are accessible via `target/site/index.html`, under the main project,
as well as each of the subprojects. (not every project has all reports)
