#!/usr/bin/env bash
#
############################################################################
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
############################################################################

REPO_ROOT=$1
for linksource in $(find $REPO_ROOT -name \*.jar); do
    linkfile=$(basename $linksource)
    linkdir=$(dirname $linksource)
    targetdir=$(realpath $REPO_ROOT/.. --relative-to=$linkdir)
    target="$targetdir/$linkfile"
    cd $linkdir
    #The copy is necessary, as maven won't add dangling symlinks to the assmebly
    cp $linkfile $target
    ln -sf $target $linkfile
done