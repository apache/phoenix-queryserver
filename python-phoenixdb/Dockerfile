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

from advian/tox-base:ubuntu-jammy-2024-10-10 as tox

###########################
# Prebuild given versions #
###########################
ARG BUILD_PYTHON_VERSIONS="3.13 3.12 3.11 3.10 3.9" #Can we take this from the parent image ?
ARG EXTRA_PYTHON_VERSIONS="3.5 3.6 3.7 3.8 2.7"
RUN export RESOLVED_VERSIONS=`pyenv_resolve $BUILD_PYTHON_VERSIONS` \
    && export EXTRA_RESOLVED_VERSIONS=`pyenv_resolve $EXTRA_PYTHON_VERSIONS` \
    && for pyver in $EXTRA_RESOLVED_VERSIONS; do pyenv install $pyver; done \
    && pyenv global $RESOLVED_VERSIONS $EXTRA_RESOLVED_VERSIONS \
    && pyenv local --unset \
    && python -m pip install -U tox \
    && apt-get update \
    && apt-get install -y krb5-user libkrb5-dev

ENV PHOENIXDB_TEST_DB_URL=http://host.docker.internal:8765
ENV PHOENIXDB_TEST_DB_TRUSTSTORE=
ENV PHOENIXDB_TEST_DB_AUTHENTICATION=
ENV PHOENIXDB_TEST_DB_AVATICA_USER=
ENV PHOENIXDB_TEST_DB_AVATICA_PASSWORD=

WORKDIR /app
VOLUME /src

COPY docker-entrypoint.sh /

ENTRYPOINT ["/docker-entrypoint.sh"]