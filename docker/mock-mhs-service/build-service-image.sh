#!/usr/bin/env bash
set -e

TAG=${1:-uk.nhs/mock-mhs-service:0.0.1-SNAPSHOT}

pushd ../../mock-mhs-service/ && ./gradlew --build-cache bootJar && popd

cp ../../mock-mhs-service/build/libs/mock-mhs-service-0.0.1-SNAPSHOT.jar mock-mhs-service.jar

docker build -t $TAG .

rm mock-mhs-service.jar
