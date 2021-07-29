#!/usr/bin/env bash

export GP2GP_SERVER_PORT="8080"
export GP2GP_AMQP_BROKERS="amqp://activemq:5672"
export GP2GP_MONGO_URI="mongodb://mongodb:27017"
export GP2GP_MONGO_DATABASE_NAME="gp2gp"
export GP2GP_MHS_OUTBOUND_URL="http://mock-mhs-adaptor:8081/mock-mhs-endpoint"
export GP2GP_GPC_GET_URL="http://gpcc:8090/@ODS_CODE@/STU3/1/gpconnect"
export GP2GP_LARGE_ATTACHMENT_THRESHOLD="15610"
export GP2GP_GPC_STRUCTURED_FHIR_BASE="/fhir/Patient/$gpc.migratestructuredrecord"

export GPC_CONSUMER_SERVER_PORT="8090"
export GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL="http://gpcc-mocks:8080"
export GPC_CONSUMER_SDS_URL="http://gpcc-mocks:8080/spine-directory/"
export GPC_ENABLE_SDS="true"
export GPC_CONSUMER_SDS_APIKEY="anykey"
export GPC_CONSUMER_LOGGING_LEVEL="DEBUG"

export GP2GP_LOGGING_LEVEL=DEBUG
export GPC_CONSUMER_LOGGING_LEVEL=DEBUG