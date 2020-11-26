#!/usr/bin/env bash
set -e

pushd service && ./build-service-image.sh
popd

docker-compose down
docker-compose up
