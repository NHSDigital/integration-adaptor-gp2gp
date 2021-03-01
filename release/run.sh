#!/bin/bash 

set -e

source ./version.sh

LIGHT_GREEN='\033[1;32m'
RED='\033[31m'
NC='\033[0m'

echo -e "${LIGHT_GREEN}Exporting environment variables in vars.sh${NC}"
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

if [ "$1" == "-n" ];
then
  echo -e "${RED}Skipping docker image pull for pre-release testing${NC}"
else
  echo -e "${LIGHT_GREEN}Pulling GP2GP adaptor image ${RELEASE_VERSION}${NC}"
  export GP2GP_IMAGE="nhsdev/nia-gp2gp-adaptor:${RELEASE_VERSION}"
  docker pull "$GP2GP_IMAGE"
fi

echo -e "${LIGHT_GREEN}Starting GP2GP adaptor ${RELEASE_VERSION}${NC}"
docker-compose up -d --no-build gp2gp

echo -e "${LIGHT_GREEN}Verify all containers are up${NC}"
docker-compose ps