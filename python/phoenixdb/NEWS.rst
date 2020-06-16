..
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

Changelog
=========

Unreleased
----------
- Replaced bundled requests_kerberos with request_gssapi library
- Use default SPNEGO Auth settings from request_gssapi
- Refactored authentication code
- Added support for specifying server certificate
- Added support for BASIC and DIGEST authentication
- Fixed HTTP error parsing
- Added transaction support
- Added list support
- Rewritten type handling
- Refactored test suite
- Removed shell example, as it was python2 only
- Updated documentation
- Added SQLAlchemy dialect

Version 0.7
-----------

- Added DictCursor for easier access to columns by their names.
- Support for Phoenix versions from 4.8 to 4.11.

Version 0.6
-----------

- Fixed result fetching when using a query with parameters.
- Support for Phoenix 4.9.

Version 0.5
-----------

- Added support for Python 3.
- Switched from the JSON serialization to Protocol Buffers, improved compatibility with Phoenix 4.8.
- Phoenix 4.6 and older are no longer supported.

Version 0.4
-----------

- Fixes for the final version of Phoenix 4.7.

Version 0.3
-----------

- Compatible with Phoenix 4.7.

Version 0.2
-----------

- Added (configurable) retry on connection errors.
- Added Vagrantfile for easier testing.
- Compatible with Phoenix 4.6.

Version 0.1
-----------

- Initial release.
- Compatible with Phoenix 4.4.
