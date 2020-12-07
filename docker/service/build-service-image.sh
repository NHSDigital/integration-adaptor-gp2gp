#!/usr/bin/env bash
set -e

TAG=${1:-uk.nhs/gp2gp}

pushd ../../

docker build -t $TAG -f docker/service/Dockerfile .

popd
