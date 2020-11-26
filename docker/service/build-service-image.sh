#!/usr/bin/env bash
set -e

TAG=${1:-uk.nhs/gp2gp:0.0.1-SNAPSHOT}

pushd ../../service/ && ./gradlew --build-cache bootJar && popd

cp ../../service/build/libs/gp2gp-0.0.1-SNAPSHOT.jar gp2gp.jar

docker build -t $TAG .

rm gp2gp.jar
