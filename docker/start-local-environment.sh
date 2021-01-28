#!/usr/bin/env bash

set -e

source ./vars.sh
docker-compose build
docker-compose up