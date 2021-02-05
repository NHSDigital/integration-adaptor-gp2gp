#!/usr/bin/env bash

set -e

LIGHT_GREEN='\033[1;32m'
NC='\033[0m'

echo -e "${LIGHT_GREEN}Exporting environment variables${NC}"
cd ../../docker || exit 1
if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "${RED}ERROR: Missing vars.sh file${NC}"
  exit 1
fi

echo -e "${LIGHT_GREEN}Building e2e test container${NC}"
docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml rm -f gp2gp-e2e-tests
docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml build gp2gp-e2e-tests

echo -e "${LIGHT_GREEN}Logging configuration for running e2e tests${NC}"
docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml config

echo -e "${LIGHT_GREEN}Running e2e tests${NC}"
docker-compose -f docker-compose.yml -f docker-compose-e2e-tests.yml up --exit-code-from gp2gp-e2e-tests --no-recreate gp2gp-e2e-tests
