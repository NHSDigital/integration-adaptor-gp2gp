source ../../docker/vars.local.vnp.sh

docker-compose -f ../../docker/docker-compose.yml up -d --build gp2gp activemq mongodb mock-mhs-adaptor;

docker-compose -f ../../docker/docker-compose-vnp.yml up -d --build gpcc-mock;
