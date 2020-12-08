#!/usr/bin/env bash
set -e

TAG=${1:-uk.nhs/mock-mhs-service:0.0.1-SNAPSHOT}

pushd ../../mocks/mhs && ./gradlew --build-cache bootJar && popd

cp ../../mocks/mhs/build/libs/mock-mhs-service-0.0.1-SNAPSHOT.jar mock-mhs-service.jar

docker build -t $TAG .

rm mock-mhs-service.jar
