#!/bin/bash 

set -e

source ./version.sh

cd ../

docker buildx build -f docker/service/Dockerfile . --platform linux/arm64/v8,linux/amd64 --tag nhsdev/nia-gp2gp-adaptor:${RELEASE_VERSION} --push

docker scout cves --only-severity critical,high --ignore-base nhsdev/nia-gp2gp-adaptor:${RELEASE_VERSION}