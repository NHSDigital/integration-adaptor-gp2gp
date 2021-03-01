#!/usr/bin/env bash

set -e

if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "No vars.sh defined. Using docker-compose defaults."
fi

docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml build gp2gp-e2e-tests
docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml up --exit-code-from gp2gp-e2e-tests gp2gp-e2e-tests