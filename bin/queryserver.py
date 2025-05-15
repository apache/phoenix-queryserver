#!/usr/bin/env python
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

#
# Script to handle launching the query server process.
#
# usage: queryserver.py [start|stop|makeWinServiceDesc] [-Dhadoop=configs]
#

from __future__ import print_function
from phoenix_queryserver_utils import tryDecode
import datetime
import getpass
import os
import os.path
import signal
import subprocess
import sys
import tempfile

try:
    import daemon
    daemon_supported = True
except ImportError:
    # daemon script not supported on some platforms (windows?)
    daemon_supported = False

def tryQuote(unquoted_input):
    """ Python 2/3 compatibility hack
    """
    try:
        from shlex import quote as cmd_quote
    except ImportError:
        from pipes import quote as cmd_quote
    return cmd_quote(unquoted_input)

import phoenix_queryserver_utils

phoenix_queryserver_utils.setPath()

command = None
args = sys.argv

if len(args) > 1:
    if args[1] == 'start':
        command = 'start'
    elif args[1] == 'stop':
        command = 'stop'
    elif args[1] == 'makeWinServiceDesc':
        command = 'makeWinServiceDesc'

if command:
    # Pull off queryserver.py and the command
    args = args[2:]
else:
    # Just pull off queryserver.py
    args = args[1:]

if os.name == 'nt':
    args = subprocess.list2cmdline(args)
else:
    # pipes module isn't available on Windows
    args = " ".join([tryQuote(tryDecode(v)) for v in args])

# HBase configuration folder path (where hbase-site.xml reside) for
# HBase/Phoenix client side property override
hbase_conf_dir = phoenix_queryserver_utils.hbase_conf_dir
hadoop_conf_dir = phoenix_queryserver_utils.hadoop_conf_dir

# TODO: add windows support
phoenix_file_basename = 'phoenix-%s-queryserver' % getpass.getuser()
phoenix_log_file = '%s.log' % phoenix_file_basename
phoenix_out_file = '%s.out' % phoenix_file_basename
phoenix_pid_file = '%s.pid' % phoenix_file_basename

tmp_dir = os.path.join(tempfile.gettempdir(), 'phoenix')
opts = os.getenv('PHOENIX_QUERYSERVER_OPTS') or os.getenv('HBASE_OPTS') or ''
pid_dir = os.getenv('PHOENIX_QUERYSERVER_PID_DIR') or os.getenv('HBASE_PID_DIR') or tmp_dir
log_dir = os.getenv('PHOENIX_QUERYSERVER_LOG_DIR') or os.getenv('HBASE_LOG_DIR') or tmp_dir
pid_file_path = os.path.join(pid_dir, phoenix_pid_file)
log_file_path = os.path.join(log_dir, phoenix_log_file)
out_file_path = os.path.join(log_dir, phoenix_out_file)

#    " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n " + \
#    " -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true" + \

# The command is run through subprocess so environment variables are automatically inherited
java_cmd = '%(java)s %(jvm_module_flags)s -cp ' +\
    hbase_conf_dir + os.pathsep + \
    hadoop_conf_dir + os.pathsep + \
    phoenix_queryserver_utils.slf4j_backend_jar + os.pathsep + \
    phoenix_queryserver_utils.logging_jar + os.pathsep + \
    phoenix_queryserver_utils.jcl_over_slf4j + os.pathsep + \
    phoenix_queryserver_utils.phoenix_client_jar + os.pathsep + \
    phoenix_queryserver_utils.phoenix_queryserver_jar + \
    " -Dproc_phoenixserver" + \
    ('' if '-Dlog4j2.configurationFile' in opts else ' -Dlog4j2.configurationFile=file:'
        + os.path.join(phoenix_queryserver_utils.current_dir, "log4j2.properties")) + \
    ('' if '-Dpsql.root.logger' in opts else " -Dpsql.root.logger=%(root_logger)s") + \
    ('' if '-Dpsql.log.dir' in opts else " -Dpsql.log.dir=%(log_dir)s") + \
    ('' if '-Dpsql.log.file' in opts else " -Dpsql.log.file=%(log_file)s") + \
    " " + opts + \
    " org.apache.phoenix.queryserver.server.QueryServer " + args

if command == 'makeWinServiceDesc':
    cmd = (java_cmd % {'java': phoenix_queryserver_utils.java,
                      'jvm_module_flags':phoenix_queryserver_utils.jvm_module_flags,
                      'root_logger': 'INFO,DRFA,console',
                      'log_dir': log_dir,
                      'log_file': phoenix_log_file})
    slices = cmd.split(' ')

    print("<service>")
    print("  <id>queryserver</id>")
    print("  <name>Phoenix Query Server</name>")
    print("  <description>This service runs the Phoenix Query Server.</description>")
    print("  <executable>%s</executable>" % slices[0])
    print("  <arguments>%s</arguments>" % ' '.join(slices[1:]))
    print("</service>")
    sys.exit()

d = os.path.dirname(out_file_path)
if not os.path.exists(d):
    os.makedirs(d)

if command == 'start':
    if not daemon_supported:
        sys.stderr.write("daemon mode not supported on this platform{}".format(os.linesep))
        sys.exit(-1)

    # get the current umask for the sub process
    current_umask = os.umask(0)
    os.umask(current_umask)

    # run in the background
    with open(out_file_path, 'a+') as out:
        context = daemon.DaemonContext(
            pidfile = daemon.PidFile(pid_file_path, 'Query Server already running, PID file found: %s' % pid_file_path),
            stdout = out,
            stderr = out,
            umask = current_umask
        )
        print('starting Query Server, logging to %s' % log_file_path)
        with context:
            # this block is the main() for the forked daemon process
            child = None
            cmd = (java_cmd % {'java': phoenix_queryserver_utils.java,
                      'jvm_module_flags':phoenix_queryserver_utils.jvm_module_flags,
                      'root_logger': 'INFO,DRFA',
                      'log_dir': log_dir,
                      'log_file': phoenix_log_file})

            # notify the child when we're killed
            def handler(signum, frame):
                if child:
                    child.send_signal(signum)
                sys.exit(0)
            signal.signal(signal.SIGTERM, handler)

            print('%s launching %s' % (datetime.datetime.now(), cmd))
            child = subprocess.Popen(cmd.split())
            sys.exit(child.wait())

elif command == 'stop':
    if not daemon_supported:
        sys.stderr.write("daemon mode not supported on this platform{}".format(os.linesep))
        sys.exit(-1)

    if not os.path.exists(pid_file_path):
        sys.stderr.write("no Query Server to stop because PID file not found, {}{}"
                         .format(pid_file_path, os.linesep))
        sys.exit(0)

    if not os.path.isfile(pid_file_path):
        sys.stderr.write("PID path exists but is not a file! {}{}".format(pid_file_path,
                                                                          os.linesep))
        sys.exit(1)

    pid = None
    with open(pid_file_path, 'r') as p:
        pid = int(p.read())
    if not pid:
        sys.exit("cannot read PID file, %s" % pid_file_path)

    print("stopping Query Server pid %s" % pid)
    with open(out_file_path, 'a+') as out:
        sys.stderr.write("sending SIGTERM{}".format(os.linesep))
    os.kill(pid, signal.SIGTERM)

else:
    # run in the foreground using defaults from log4j.properties
    cmd = (java_cmd % {'java': phoenix_queryserver_utils.java,
                      'jvm_module_flags':phoenix_queryserver_utils.jvm_module_flags,
                      'root_logger': 'INFO,console',
                      'log_dir': '.',
                      'log_file': 'psql.log'})
    splitcmd = cmd.split()
    os.execvp(splitcmd[0], splitcmd)
