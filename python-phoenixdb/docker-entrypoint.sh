#!/bin/bash -l

set -e
if [ "$#" -eq 0 ]; then
  find /src -mindepth 1 -maxdepth 1 \( -type d -name ".*" -prune \) -o -exec cp -r --target-directory=/app -- {} +
  # Then run tox
  tox
else
  exec "$@"
fi
