#!/usr/bin/env bash

export GP2GP_SERVER_PORT="8080"
export GP2GP_AMQP_BROKERS="amqp://activemq:5672"
export GP2GP_MONGO_URI="mongodb://mongodb:27017"
export GP2GP_MONGO_DATABASE_NAME="gp2gp"
export GP2GP_MHS_MOCK_BASE_URL="http://mock-mhs-adaptor:8081"
export GP2GP_MHS_OUTBOUND_URL="$GP2GP_MHS_MOCK_BASE_URL/mock-mhs-endpoint"
export GP2GP_GPC_GET_URL="http://gpcc-mock:8080/@ODS_CODE@/STU3/1/gpconnect"
export GP2GP_LARGE_ATTACHMENT_THRESHOLD="31216"
export GP2GP_LOGGING_LEVEL="INFO"

export GPC_CONSUMER_SERVER_PORT="8090"
export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="http://gpc-api-mock:8080"
export GPC_CONSUMER_SDS_URL="http://sds-api-mock:8080"
export GPC_ENABLE_SDS="true"
export GPC_CONSUMER_SDS_APIKEY="anykey"
export GPC_CONSUMER_LOGGING_LEVEL="INFO"




