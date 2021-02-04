#!/bin/bash 

RELEASE_VERSION=0.0.1
export GP2GP_IMAGE="nhsdev/nia-gp2gp-adaptor:${RELEASE_VERSION}"

LIGHT_GREEN='\033[1;32m' 
NC='\033[0m'

echo -e "${LIGHT_GREEN}Exporting environment variables${NC}"
source vars.sh

echo -e "${LIGHT_GREEN}Following gp2gp container logs${NC}"
cd ../docker || exit 1
docker-compose logs -f gp2gp
