#!/usr/bin/env bash

set -e

if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "No vars.sh define. Using docker-compose defaults."
fi
if [[ "$(docker network ls | grep "commonforgp2gp")" == "" ]] ; then
    docker network create commonforgp2gp
fi
docker-compose -f docker-compose-arm.yml down --rmi=local --remove-orphans
docker-compose -f docker-compose-arm.yml rm
docker-compose -f docker-compose-arm.yml build
docker-compose -f docker-compose-arm.yml up mock-mhs-adaptor mongodb activemq wiremock gpcc gp2gp
docker network rm commonforgp2gp