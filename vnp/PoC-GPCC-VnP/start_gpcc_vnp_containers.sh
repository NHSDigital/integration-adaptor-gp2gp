source ../../docker/vars.local.vnp.sh

docker-compose -f ../../docker/docker-compose.yml up -d --build gpcc;

docker-compose -f ../../docker/docker-compose-vnp.yml up -d --build gpc-api-mock sds-api-mock;
