#!/usr/bin/env bash

set -e

if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "No vars.sh define. Using docker-compose defaults."
fi

docker-compose down
docker-compose build
docker-compose up