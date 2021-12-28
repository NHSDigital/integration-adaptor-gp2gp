#!/usr/bin/env bash

export GP2GP_SERVER_PORT="8080"
export GP2GP_AMQP_BROKERS="amqp://activemq:5672"
export GP2GP_MONGO_URI="mongodb://mongodb:27017"
export GP2GP_MONGO_DATABASE_NAME="gp2gp"
export GP2GP_MHS_OUTBOUND_URL="http://mock-mhs-adaptor:8081/mock-mhs-endpoint"
export GP2GP_GPC_GET_URL="http://gpcc:8090/@ODS_CODE@/STU3/1/gpconnect"

export GPC_CONSUMER_SERVER_PORT="8090"
export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="http://tkw:4854"
export GPC_CONSUMER_SDS_APIKEY="anykey"
export GPC_CONSUMER_LOGGING_LEVEL="DEBUG"

export GP2GP_LOGGING_LEVEL=DEBUG
export GPC_CONSUMER_LOGGING_LEVEL=DEBUG
