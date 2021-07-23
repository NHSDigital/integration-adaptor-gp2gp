#!/usr/bin/env bash

set -e

if [ -f "vars.sh" ]; then
    source vars.sh
else
  echo "No vars.sh define. Using docker-compose defaults."
fi

docker network create commonforgp2gp
docker-compose down --rmi=local --remove-orphans
docker-compose rm
docker-compose build
docker-compose up gp2gp mock-mhs-adaptor mongodb activemq gpcc tkw gpconnect-api gpconnect-db
docker network rm commonforgp2gp