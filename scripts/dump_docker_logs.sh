#!/bin/bash

mkdir -p ./logs

container_names=$(docker ps --format '{{.Names}}')

for container in $container_names; do
    docker logs "$container" > ./logs/"$container".log
    echo "Logs saved for container: $container"
done