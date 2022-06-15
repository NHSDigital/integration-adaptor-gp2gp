#!/usr/bin/env bash

set -e

if [ -f "../docker/vars.sh" ]; then
  cd ../smoke-tests
    source ../docker/vars.sh; ./gradlew smokeTest
else
  echo "No vars.sh defined."
fi