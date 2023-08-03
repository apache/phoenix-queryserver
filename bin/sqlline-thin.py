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

from __future__ import print_function
from phoenix_queryserver_utils import tryDecode
import os
import subprocess
import sys
import phoenix_queryserver_utils
import atexit
try:
    import urlparse
    parse_url = urlparse.urlparse
except ImportError:
    import urllib.parse
    parse_url = urllib.parse.urlparse

# import argparse
try:
    import argparse
except ImportError:
    current_dir = os.path.dirname(os.path.abspath(__file__))
    sys.path.append(os.path.join(current_dir, 'argparse-1.4.0'))
    import argparse

global childProc
childProc = None
def kill_child():
    if childProc is not None:
        childProc.terminate()
        childProc.kill()
        if os.name != 'nt':
            os.system("reset")
atexit.register(kill_child)

parser = argparse.ArgumentParser(description='Launches the Apache Phoenix Thin Client.')
# Positional argument "url" is optional
parser.add_argument('url', nargs='?', help='The URL to the Phoenix Query Server.', default='http://localhost:8765')
# Positional argument "sqlfile" is optional
parser.add_argument('sqlfile', nargs='?', help='A file of SQL commands to execute.', default='')
# Avatica wire authentication
parser.add_argument('-a', '--authentication', help='Mechanism for HTTP authentication.', choices=('SPNEGO', 'BASIC', 'DIGEST', 'NONE'), default='')
# Avatica wire serialization
parser.add_argument('-s', '--serialization', help='Serialization type for HTTP API.', choices=('PROTOBUF', 'JSON'), default=None)
# Avatica authentication
parser.add_argument('-au', '--auth-user', help='Username for HTTP authentication.')
parser.add_argument('-ap', '--auth-password', help='Password for HTTP authentication.')
# Avatica principal and keytab
parser.add_argument('-p', '--principal', help='Kerberos principal for SPNEGO authenction from keytab.')
parser.add_argument('-kt', '--keytab', help='Kerberos keytab file for SPNEGO authenction from keytab.')
# Avatica HTTPS truststore
parser.add_argument('-t', '--truststore', help='Truststore file that contains the TLS certificate of the server.')
parser.add_argument('-tp', '--truststore-password', help='Password for the server TLS certificate truststore')
# Keystore type
parser.add_argument('-st', '--keystore-type', help='Type of key- and truststore files (i.e. JKS).')

# Common arguments across sqlline.py and sqlline-thin.py
phoenix_queryserver_utils.common_sqlline_args(parser)
# Parse the args
args=parser.parse_args()

phoenix_queryserver_utils.setPath()

url = tryDecode(args.url)
sqlfile = tryDecode(args.sqlfile)

serialization_key = 'phoenix.queryserver.serialization'
default_serialization='PROTOBUF'
hbase_authentication_key = 'hbase.security.authentication'
default_hbase_authentication = ''
spnego_auth_disabled_key = 'phoenix.queryserver.spnego.auth.disabled'
default_spnego_auth_disabled = 'false'

def cleanup_url(url):
    parsed = parse_url(url)
    if parsed.scheme == "":
        url = "http://" + url
        parsed = parse_url(url)
    if ":" not in parsed.netloc:
        url = url + ":8765"
    return url

def get_hbase_param(key, default):
    env=os.environ.copy()
    if os.name == 'posix':
      hbase_exec_name = 'hbase'
    elif os.name == 'nt':
      hbase_exec_name = 'hbase.cmd'
    else:
      print('Unknown platform "%s", defaulting to HBase executable of "hbase"' % os.name)
      hbase_exec_name = 'hbase'

    hbase_cmd = phoenix_queryserver_utils.which(hbase_exec_name)
    if hbase_cmd is None:
        print('Failed to find hbase executable on PATH, defaulting %s to %s.' % (key, default))
        return default

    env['HBASE_CONF_DIR'] = phoenix_queryserver_utils.hbase_conf_dir
    proc = subprocess.Popen([hbase_cmd, 'org.apache.hadoop.hbase.util.HBaseConfTool', key],
            env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (stdout, stderr) = proc.communicate()
    if proc.returncode != 0:
        print('Failed to extract %s from hbase-site.xml, defaulting to %s.' % (key, default))
        return default
    # Don't expect this to happen, but give a default value just in case
    if stdout is None:
        return default

    stdout = tryDecode(stdout.strip())
    if stdout == 'null':
        return default
    return stdout

def get_serialization():
    return get_hbase_param(serialization_key, default_serialization)

def get_hbase_authentication():
    return get_hbase_param(hbase_authentication_key, default_hbase_authentication)

def get_spnego_auth_disabled():
    return get_hbase_param(spnego_auth_disabled_key, default_spnego_auth_disabled)

url = cleanup_url(url)

if sqlfile != "":
    sqlfile = "--run=" + sqlfile

colorSetting = tryDecode(args.color)
# disable color setting for windows OS
if os.name == 'nt':
    colorSetting = "false"

if os.uname()[4].startswith('ppc'):
    disable_jna = " -Dorg.jline.terminal.jna=false "
else:
    disable_jna = ""

# HBase configuration folder path (where hbase-site.xml reside) for
# HBase/Phoenix client side property override
hbase_config_path = os.getenv('HBASE_CONF_DIR', phoenix_queryserver_utils.current_dir)

serialization = tryDecode(args.serialization) if args.serialization else get_serialization()

java_home = os.getenv('JAVA_HOME')

# load hbase-env.??? to extract JAVA_HOME, HBASE_PID_DIR, HBASE_LOG_DIR
hbase_env_path = None
hbase_env_cmd  = None
if os.name == 'posix':
    hbase_env_path = os.path.join(hbase_config_path, 'hbase-env.sh')
    hbase_env_cmd = ['bash', '-c', 'source %s && env' % hbase_env_path]
elif os.name == 'nt':
    hbase_env_path = os.path.join(hbase_config_path, 'hbase-env.cmd')
    hbase_env_cmd = ['cmd.exe', '/c', 'call %s & set' % hbase_env_path]
if not hbase_env_path or not hbase_env_cmd:
    sys.stderr.write("hbase-env file unknown on platform {}{}".format(os.name, os.linesep))
    sys.exit(-1)

hbase_env = {}
if os.path.isfile(hbase_env_path):
    p = subprocess.Popen(hbase_env_cmd, stdout = subprocess.PIPE)
    for x in p.stdout:
        (k, _, v) = tryDecode(x).partition('=')
        hbase_env[k.strip()] = v.strip()

if 'JAVA_HOME' in hbase_env:
    java_home = hbase_env['JAVA_HOME']

if java_home:
    java = os.path.join(java_home, 'bin', 'java')
else:
    java = 'java'

jdbc_url = 'jdbc:phoenix:thin:url=' + url + ';serialization=' + serialization
if args.authentication:
    jdbc_url += ';authentication=' + tryDecode(args.authentication)
if args.auth_user:
    jdbc_url += ';avatica_user=' + tryDecode(args.auth_user)
if args.auth_password:
    jdbc_url += ';avatica_password=' + tryDecode(args.auth_password)
if args.principal:
    jdbc_url += ';principal=' + tryDecode(args.principal)
if args.keytab:
    jdbc_url += ';keytab=' + tryDecode(args.keytab)
if args.truststore:
    jdbc_url += ';truststore=' + tryDecode(args.truststore)
if args.truststore_password:
    jdbc_url += ';truststore_password=' + tryDecode(args.truststore_password)
if args.keystore_type:
    jdbc_url += ';keystore_type=' + tryDecode(args.keystore_type)

# Add SPENGO auth if this cluster uses it, and there are no conflicting HBase parameters
if (get_hbase_authentication() == 'kerberos' and get_spnego_auth_disabled() == 'false'
   and 'authentication=' not in jdbc_url and 'avatica_user=' not in jdbc_url):
    jdbc_url += ';authentication=SPNEGO'

java_cmd = phoenix_queryserver_utils.java + ' ' + phoenix_queryserver_utils.jvm_module_flags + \
    ' $PHOENIX_OPTS ' + \
    ' -cp "' + phoenix_queryserver_utils.sqlline_with_deps_jar + os.pathsep + \
    phoenix_queryserver_utils.phoenix_thin_client_jar + os.pathsep + \
    phoenix_queryserver_utils.slf4j_backend_jar + os.pathsep + \
    phoenix_queryserver_utils.logging_jar +\
    '" -Dlog4j2.configurationFile=file:' + os.path.join(phoenix_queryserver_utils.current_dir, "log4j2.properties") + \
    ' -Djavax.security.auth.useSubjectCredsOnly=false ' + \
    disable_jna + \
    " org.apache.phoenix.queryserver.client.SqllineWrapper -d org.apache.phoenix.queryserver.client.Driver " + \
    ' -u "' + jdbc_url + '"' + " -n none -p none " + \
    " --color=" + colorSetting + " --fastConnect=" + tryDecode(args.fastconnect) + " --verbose=" + tryDecode(args.verbose) + \
    " --incremental=false --isolation=TRANSACTION_READ_COMMITTED " + sqlfile

os.execl("/bin/sh", "/bin/sh", "-c", java_cmd)
