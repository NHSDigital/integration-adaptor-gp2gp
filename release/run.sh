#!/bin/bash 

source ./version.sh

LIGHT_GREEN='\033[1;32m'
RED='\033[31m'
NC='\033[0m'

echo -e "${LIGHT_GREEN}Exporting environment variables${NC}"
cd ../docker || exit 1
if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "${RED}ERROR: Missing vars.sh file${NC}"
  exit 1
fi

echo -e "${LIGHT_GREEN}Stopping running containers${NC}"
docker-compose down

echo -e "${LIGHT_GREEN}Building and starting dependencies${NC}"
docker-compose up -d activemq mongodb wiremock mock-mhs-adaptor

echo -e "${LIGHT_GREEN}Starting GP2GP adaptor ${RELEASE_VERSION}${NC}"
export GP2GP_IMAGE="nhsdev/nia-gp2gp-adaptor:${RELEASE_VERSION}"
docker-compose up -d --no-build gp2gp

echo -e "${LIGHT_GREEN}Verify all containers are up${NC}"
docker-compose ps