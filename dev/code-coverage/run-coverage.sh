#!/usr/bin/env bash
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

usage() {
  echo
  echo "options:"
  echo "  -h     Display help"
  echo "  -u     SonarQube Host URL"
  echo "  -l     SonarQube Login Credentials"
  echo "  -k     SonarQube Project Key"
  echo "  -n     SonarQube Project Name"
  echo "  -t     Number of threads (example: 1 or 2C)."
  echo
  echo "Important:"
  echo "  The required parameters for publishing the coverage results to SonarQube:"
  echo "    - Host URL"
  echo "    - Login Credentials"
  echo "    - Project Key"
  echo
}

execute() {
  SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
  MAIN_POM="${SCRIPT_DIR}/../../pom.xml"
  # Check the syntax for the THREAD_COUNT variable
  if [[ "$THREAD_COUNT" =~ ^[0-9]+([.][0-9]+)?$ ]] || [[ "$THREAD_COUNT" =~ ^[0-9]+([.][0-9]+)+[C]?$ ]]; then
    THREADS="${THREAD_COUNT}"
  else
    THREADS=1
  fi

  mvn -B -e -f "$MAIN_POM" clean verify -Pcodecoverage -fn -Dmaven.javadoc.skip=true -DskipShade -T "$THREADS"

  # If the required parameters are given, the code coverage results are uploaded to the SonarQube Server
  if [ -n "$SONAR_LOGIN" ] && [ -n "$SONAR_PROJECT_KEY" ] && [ -n "$SONAR_URL" ]; then
    mvn -B -e -Pcodecoverage -f "$MAIN_POM" sonar:sonar -Dsonar.projectName="$SONAR_PROJECT_NAME" \
      -Dsonar.host.url="$SONAR_URL" -Dsonar.login="$SONAR_LOGIN" -Dsonar.projectKey="$SONAR_PROJECT_KEY" -T "$THREADS"
  fi
}

while getopts ":u:l:k:n:t:h" option; do
  case $option in
  u) SONAR_URL=${OPTARG:-} ;;
  l) SONAR_LOGIN=${OPTARG:-} ;;
  k) SONAR_PROJECT_KEY=${OPTARG:-} ;;
  n) SONAR_PROJECT_NAME=${OPTARG:-} ;;
  t) THREAD_COUNT=${OPTARG:-} ;;
  h) # Display usage
    usage
    exit
    ;;
  \?) # Invalid option
    echo "Error: Invalid option"
    exit
    ;;
  esac
done

# Start code analysis
execute
