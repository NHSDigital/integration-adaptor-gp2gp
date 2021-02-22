#!/bin/bash 

LIGHT_GREEN='\033[1;32m' 
NC='\033[0m'

echo -e "${LIGHT_GREEN}Following gp2gp container logs${NC}"
cd ../docker || exit 1
docker-compose logs -f gp2gp
