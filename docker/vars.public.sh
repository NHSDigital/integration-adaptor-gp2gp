#!/usr/bin/env bash

export GP2GP_SERVER_PORT="8080"
export GP2GP_AMQP_BROKERS="amqp://activemq:5672"
export GP2GP_MONGO_URI="mongodb://mongodb:27017"
export GP2GP_MONGO_DATABASE_NAME="gp2gp"
export GP2GP_MHS_OUTBOUND_URL="http://mock-mhs-adaptor:8081/mock-mhs-endpoint"
export GP2GP_GPC_GET_URL="https://gpcc:8000/B82617/STU3/1/gpconnect"

export GPC_CONSUMER_URL="http://gpcc:8000"
export GPC_CONSUMER_GPC_GET_URL="https://orange.testlab.nhs.uk"
export GPC_CONSUMER_SDS_URL="http://wiremock:8080/"
