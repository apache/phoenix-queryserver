# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest
import phoenixdb
import phoenixdb.cursor
from phoenixdb.errors import InternalError
from phoenixdb.tests import TEST_DB_URL
from phoenixdb.tests import DatabaseTestCase


@unittest.skipIf(TEST_DB_URL is None, "these tests require the PHOENIXDB_TEST_DB_URL environment variable set to a clean database")
class PhoenixDatabaseTest(DatabaseTestCase):

    def test_select_literal(self):
        with self.conn.cursor() as cursor:
            self.createTable("test", "CREATE TABLE {table} (id INTEGER PRIMARY KEY, text VARCHAR)")
            cursor.executemany("UPSERT INTO test VALUES (?, ?)", [[i, 'text {}'.format(i)] for i in range(10)])

        with self.conn.cursor() as cursor:
            cursor.itersize = 4
            cursor.execute("SELECT * FROM test WHERE id>1 ORDER BY id")
            self.assertEqual(cursor.fetchall(), [[i, 'text {}'.format(i)] for i in range(2, 10)])

    def test_select_parameter(self):
        with self.conn.cursor() as cursor:
            self.createTable("test", "CREATE TABLE {table} (id INTEGER PRIMARY KEY, text VARCHAR)")
            cursor.executemany("UPSERT INTO test VALUES (?, ?)", [[i, 'text {}'.format(i)] for i in range(10)])

        with self.conn.cursor() as cursor:
            cursor.itersize = 4
            cursor.execute("SELECT * FROM test WHERE id>? ORDER BY id", [1])
            self.assertEqual(cursor.fetchall(), [[i, 'text {}'.format(i)] for i in range(2, 10)])

    def _check_dict_cursor(self, cursor):
        self.createTable("test", "CREATE TABLE {table} (id INTEGER PRIMARY KEY, text VARCHAR)")
        cursor.execute("UPSERT INTO test VALUES (?, ?)", [1, 'text 1'])
        cursor.execute("SELECT * FROM test ORDER BY id")
        self.assertEqual(cursor.fetchall(), [{'ID': 1, 'TEXT': 'text 1'}])

    def test_dict_cursor_default_parameter(self):
        self.reopen(autocommit=True, cursor_factory=phoenixdb.cursor.DictCursor)

        with self.conn.cursor() as cursor:
            self._check_dict_cursor(cursor)

    def test_dict_cursor_default_attribute(self):
        db = phoenixdb.connect(TEST_DB_URL, autocommit=True)
        db.cursor_factory = phoenixdb.cursor.DictCursor
        self.addCleanup(db.close)

        with db.cursor() as cursor:
            self._check_dict_cursor(cursor)

    def test_dict_cursor(self):
        db = phoenixdb.connect(TEST_DB_URL, autocommit=True)
        self.addCleanup(db.close)

        with db.cursor(cursor_factory=phoenixdb.cursor.DictCursor) as cursor:
            self._check_dict_cursor(cursor)

    def test_schema(self):

        with self.conn.cursor() as cursor:
            try:
                cursor.execute("CREATE SCHEMA IF NOT EXISTS test_schema")
            except InternalError as e:
                if "phoenix.schema.isNamespaceMappingEnabled" in e.message:
                    self.skipTest(e.message)
                raise

            self.createTable("test_schema.test", "CREATE TABLE {table} (id INTEGER PRIMARY KEY, text VARCHAR)")
            cursor.execute("UPSERT INTO test_schema.test VALUES (?, ?)", [1, 'text 1'])
            cursor.execute("SELECT * FROM test_schema.test ORDER BY id")
            self.assertEqual(cursor.fetchall(), [[1, 'text 1']])

    def test_transaction(self):
        db = phoenixdb.connect(TEST_DB_URL, autocommit=True)