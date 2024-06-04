Changelog
=========

Version 1.2.2
-------------
- Update python-phoenixdb/RELEASING.rst (PHOENIX-6820)
- Crash Due to Unhandled JDBC Type Code 0 for NULL Values (PHOENIX-7246)
- Manage requests-gssapi version for Phython 3.7 and lower (PHOENIX-7221)
- Add Python 3.11 to supported languages and update docker test image for phoenixdb (PHOENIX-6858)
- Add Python 3.12 to supported versions and the test matrix (PHOENIX-7222)
- Document workaround for PhoenixDB 1.2+ not working with Python2 on some systems (PHOENIX-6863)
- Update install instructions in README.rst (PHOENIX-6812)
- Add support for SQLAlchemy 2.0 (PHOENIX-6892)
- SQLAlchemy is no longer an install dependency (PHOENIX-6892)
- Run tests with all supported Python + SqlAlchemy versions (1.3, 1.4, 2.0) (PHOENIX-6892)
- Replace deprecated failUnless methods in tests (PHOENIX-6892)
- Add support for specifying custom HTTP headers (PHOENIX-6921)
- Use JDBC/Avatica column label as column name when set (PHOENIX-6917)
- Do not throw exception when shutting down Python with open connections (PHOENIX-6926)
- Fix twine check error and add a steps to Releasing guide (PHOENIX-7323)

Version 1.2.1
-------------
- Defined authentication mechanism for SPNEGO explicitly (PHOENIX-6781)
- Fixed failing docker build because of missing files (PHOENIX-6801)
- Fixed make_rc.sh script on mac (PHOENIX-6803)
- Fixed flaky tests

Version 1.2.0
-------------

- Updated test environment to support Python 3.9 and 3.10 (PHOENIX-6737)
- Fixed get_view_names() in SqlAlchemy driver (PHOENIX-6727)
- Added supports_statement_cache attribute for SqlAlchemy dialect to avoid warnings (PHOENIX-6735)
- Re-generated the protobuf Python files with protoc 3.19 from the Avatica 1.21 descriptors (PHOENIX-6731)
- Re-added phoenixdb requirements (PHOENIX-6811)
- Dropped support for Python 3.4 (PHOENIX-6731)

Version 1.1.0
-------------

- Implemented get_primary_keys() and get_index_info() methods in meta object (PHOENIX-6410)
- Fixed broken SqlAlchemy get_pk_constraint() method to use meta.get_primary_keys() (PHOENIX-6410)
- Implemented SqlAlchemy get_indexes() method to expose meta.get_index_info() (PHOENIX-6410)
- Fixed empty array column handling in result set (PHOENIX-6484)

Version 1.0.1
-------------

- Use HTTP sessions to enable sticky load balancers (PHOENIX-6459)
- Revert default GSSAPI OID to SPNEGO to improve compatibility (PHOENIX-6414)

Version 1.0.0
-------------

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
- Implemented Avatica Metadata API
- Misc fixes
- Licensing cleanup

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
