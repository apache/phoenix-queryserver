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

import collections
import logging

from phoenixdb.avatica.proto import common_pb2
from phoenixdb.errors import InternalError, ProgrammingError
from phoenixdb.types import TypeHelper

__all__ = ['Cursor', 'ColumnDescription', 'DictCursor']

logger = logging.getLogger(__name__)

# TODO see note in Cursor.rowcount()
MAX_INT = 2 ** 64 - 1

ColumnDescription = collections.namedtuple('ColumnDescription', 'name type_code display_size internal_size precision scale null_ok')
"""Named tuple for representing results from :attr:`Cursor.description`."""


class Cursor(object):
    """Database cursor for executing queries and iterating over results.

    You should not construct this object manually, use :meth:`Connection.cursor() <phoenixdb.connection.Connection.cursor>` instead.
    """

    arraysize = 1
    """
    Read/write attribute specifying the number of rows to fetch
    at a time with :meth:`fetchmany`. It defaults to 1 meaning to
    fetch a single row at a time.
    """

    itersize = 2000
    """
    Read/write attribute specifying the number of rows to fetch
    from the backend at each network roundtrip during iteration
    on the cursor. The default is 2000.
    """

    def __init__(self, connection, id=None):
        self._connection = connection
        self._id = id
        self._signature = None
        self._column_data_types = []
        self._frame = None
        self._pos = None
        self._closed = False
        self.arraysize = self.__class__.arraysize
        self.itersize = self.__class__.itersize
        self._updatecount = -1

    def __del__(self):
        if not self._connection._closed and not self._closed:
            self.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        if not self._closed:
            self.close()

    def __iter__(self):
        return self

    def __next__(self):
        row = self.fetchone()
        if row is None:
            raise StopIteration
        return row

    next = __next__

    def close(self):
        """Closes the cursor.
        No further operations are allowed once the cursor is closed.

        If the cursor is used in a ``with`` statement, this method will
        be automatically called at the end of the ``with`` block.
        """
        if self._closed:
            raise ProgrammingError('The cursor is already closed.')
        if self._id is not None:
            self._connection._client.close_statement(self._connection._id, self._id)
            self._id = None
        self._signature = None
        self._column_data_types = []
        self._frame = None
        self._pos = None
        self._closed = True

    @property
    def closed(self):
        """Read-only attribute specifying if the cursor is closed or not."""
        return self._closed

    @property
    def description(self):
        if self._signature is None:
            return None
        description = []
        for column in self._signature.columns:
            description.append(ColumnDescription(
                self._get_column_name(column),
                column.type.name,
                column.display_size,
                None,
                column.precision,
                column.scale,
                None if column.nullable == 2 else bool(column.nullable),
            ))
        return description

    def _get_column_name(self, column):
        if column.label:
            # Not empty
            return column.label
        else:
            return column.column_name

    def _set_id(self, id):
        if self._id is not None and self._id != id:
            self._connection._client.close_statement(self._connection._id, self._id)
        self._id = id

    def _set_signature(self, signature):
        self._signature = signature
        self._column_data_types = []
        self._parameter_data_types = []
        if signature is None:
            return

        for column in signature.columns:
            dtype = TypeHelper.from_column(column)
            self._column_data_types.append(dtype)

        for parameter in signature.parameters:
            dtype = TypeHelper.from_param(parameter)
            self._parameter_data_types.append(dtype)

    def _set_frame(self, frame):
        self._frame = frame
        self._pos = None

        if frame is not None:
            if frame.rows:
                self._pos = 0
            elif not frame.done:
                raise InternalError('Got an empty frame, but the statement is not done yet.')

    def _fetch_next_frame(self):
        offset = self._frame.offset + len(self._frame.rows)
        frame = self._connection._client.fetch(
            self._connection._id, self._id,
            offset=offset, frame_max_size=self.itersize)
        self._set_frame(frame)

    def _process_result(self, result):
        if result.own_statement:
            self._set_id(result.statement_id)
        self._set_signature(result.signature if result.HasField('signature') else None)
        self._set_frame(result.first_frame if result.HasField('first_frame') else None)
        self._updatecount = result.update_count

    def _process_results(self, results):
        if results:
            return self._process_result(results[0])

    def _transform_parameters(self, parameters):
        if len(parameters) != len(self._parameter_data_types):
            raise ProgrammingError('Number of placeholders (?) must match number of parameters.'
                                   ' Number of placeholders: {0}. Number of parameters: {1}'
                                   .format(len(self._parameter_data_types), len(parameters)))
        typed_parameters = []
        for value, data_type in zip(parameters, self._parameter_data_types):
            field_name, rep, mutate_to, cast_from, is_array = data_type
            typed_value = common_pb2.TypedValue()

            if value is None:
                typed_value.null = True
                typed_value.type = common_pb2.NULL
            else:
                typed_value.null = False
                if is_array:
                    if type(value) in [list, tuple]:
                        for element in value:
                            if mutate_to is not None:
                                element = mutate_to(element)
                            typed_element = common_pb2.TypedValue()
                            if element is None:
                                typed_element.null = True
                            else:
                                typed_element.type = rep
                                setattr(typed_element, field_name, element)
                            typed_value.array_value.append(typed_element)
                        typed_value.type = common_pb2.ARRAY
                        typed_value.component_type = rep
                    else:
                        raise ProgrammingError('Scalar value specified for array parameter.')
                else:
                    if mutate_to is not None:
                        value = mutate_to(value)
                    typed_value.type = rep
                    setattr(typed_value, field_name, value)

            typed_parameters.append(typed_value)
        return typed_parameters

    def execute(self, operation, parameters=None):
        if self._closed:
            raise ProgrammingError('The cursor is already closed.')
        self._updatecount = -1
        self._set_frame(None)
        if parameters is None:
            if self._id is None:
                self._set_id(self._connection._client.create_statement(self._connection._id))
            results = self._connection._client.prepare_and_execute(
                self._connection._id, self._id,
                operation, first_frame_max_size=self.itersize)
            self._process_results(results)
        else:
            statement = self._connection._client.prepare(
                self._connection._id, operation)
            self._set_id(statement.id)
            self._set_signature(statement.signature)

            results = self._connection._client.execute(
                self._connection._id, self._id,
                statement.signature, self._transform_parameters(parameters),
                first_frame_max_size=self.itersize)
            self._process_results(results)

    def executemany(self, operation, seq_of_parameters):
        if self._closed:
            raise ProgrammingError('The cursor is already closed.')
        self._updatecount = -1
        self._set_frame(None)
        statement = self._connection._client.prepare(
            self._connection._id, operation, max_rows_total=0)
        self._set_id(statement.id)
        self._set_signature(statement.signature)
        return self._connection._client.execute_batch(
            self._connection._id, self._id,
            [self._transform_parameters(p) for p in seq_of_parameters])

    def get_sync_results(self, state):
        if self._closed:
            raise ProgrammingError('The cursor is already closed.')
        if self._id is None:
            self._set_id(self._connection._client.create_statement(self._connection._id))
        return self._connection._client.get_sync_results(self._connection._id, self._id, state)

    def fetch(self, signature):
        if self._closed:
            raise ProgrammingError('The cursor is already closed.')
        self._updatecount = -1
        self._set_signature(signature)
        frame = self._connection._client.fetch(self._connection._id, self._id, 0, self.itersize)
        self._set_frame(frame)

    def _transform_row(self, row):
        """Transforms a Row into Python values.

        :param row:
            A ``common_pb2.Row`` object.

        :returns:
            A list of values casted into the correct Python types.

        :raises:
            NotImplementedError
        """
        tmp_row = []

        for i, column in enumerate(row.value):
            if column.scalar_value.null:
                tmp_row.append(None)
            elif column.has_array_value:
                field_name, rep, mutate_to, cast_from = self._column_data_types[i]

                list_value = []
                for j, typed_value in enumerate(column.array_value):
                    value = getattr(typed_value, field_name)
                    if cast_from is not None:
                        value = cast_from(value)
                    list_value.append(value)

                tmp_row.append(list_value)
            else:
                field_name, rep, mutate_to, cast_from = self._column_data_types[i]

                # get the value from the field_name
                value = getattr(column.scalar_value, field_name)

                # cast the value
                if cast_from is not None:
                    value = cast_from(value)

                tmp_row.append(value)
        return tmp_row

    def fetchone(self):
        if self._frame is None:
            raise ProgrammingError('No select statement was executed.')
        if self._pos is None:
            return None
        rows = self._frame.rows
        row = self._transform_row(rows[self._pos])
        self._pos += 1
        if self._pos >= len(rows):
            self._pos = None
            if not self._frame.done:
                self._fetch_next_frame()
        return row

    def fetchmany(self, size=None):
        if size is None:
            size = self.arraysize
        rows = []
        while size > 0:
            row = self.fetchone()
            if row is None:
                break
            rows.append(row)
            size -= 1
        return rows

    def fetchall(self):
        rows = []
        while True:
            row = self.fetchone()
            if row is None:
                break
            rows.append(row)
        return rows

    def setinputsizes(self, sizes):
        pass

    def setoutputsize(self, size, column=None):
        pass

    @property
    def connection(self):
        """Read-only attribute providing access to the :class:`Connection <phoenixdb.connection.Connection>`
        object this cursor was created from."""
        return self._connection

    @property
    def rowcount(self):
        """Read-only attribute specifying the number of rows affected by
        the last executed DML statement or -1 if the number cannot be
        determined. Note that this will always be set to -1 for select
        queries."""
        # TODO instead of -1, this ends up being set to Integer.MAX_VALUE
        if self._updatecount == MAX_INT:
            return -1
        return self._updatecount

    @property
    def rownumber(self):
        """Read-only attribute providing the current 0-based index of the
        cursor in the result set or ``None`` if the index cannot be
        determined.

        The index can be seen as index of the cursor in a sequence
        (the result set). The next fetch operation will fetch the
        row indexed by :attr:`rownumber` in that sequence.
        """
        if self._frame is not None and self._pos is not None:
            return self._frame.offset + self._pos
        return self._pos


class DictCursor(Cursor):
    """A cursor which returns results as a dictionary"""

    def _transform_row(self, row):
        row = super(DictCursor, self)._transform_row(row)
        d = {}
        for ind, val in enumerate(row):
            d[self._get_column_name(self._signature.columns[ind])] = val
        return d
