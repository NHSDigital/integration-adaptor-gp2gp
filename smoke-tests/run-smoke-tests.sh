#!/usr/bin/env bash

set -x -e

if [ -f "$1" ]; then

  cp "$1" smoke_vars.sh

  source smoke_vars.sh; ./gradlew smokeTest
else
  echo "Missing argument. The location of your vars.sh file should be passed as the first argument"
fi