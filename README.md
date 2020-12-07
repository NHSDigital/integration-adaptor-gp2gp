# integration-adaptor-gp2gp
National Integration Adaptor - GP2GP

The existing GP2GP solution is based on a legacy messaging standard and infrastructure (HL7v3 and Spine TMS). Reliance on these standards going forward presents a significant barrier to successful GP2GP implementation by new suppliers, and perpetuation of these standards in the long term presents a risk to the continued operation of GP2GP across all suppliers.

A hybrid solution approach has been selected as the best option for GP2GP adoption by NMEs and transition by existing incumbent suppliers.

The "National Integration Adaptor - GP2GP" implements a GP2GP 2.2 producer using the supplier's existing GP Connect Provider implementation to extract the Electronic Health Record. Suppliers that have not already implemented a GP2GP 2.2 producer, or those wishing to decommission their existing producer, may deploy the GP2GP adaptor in its place.

## Requirements:
1. JDK 11

## Configuration

The adaptor reads its configuration from environment variables. The following sections describe the environment variables
 used to configure the adaptor. 
 
Variables without a default value and not marked optional, *MUST* be defined for the adaptor to run.

### General Configuration Options

| Environment Variable               | Default                   | Description 
| -----------------------------------|---------------------------|-------------
| GP2GP_SERVER_PORT                  | 9000                      | The port on which the GP2GP API will run
| GP2GP_LOGGING_LEVEL                | INFO                      | Application logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data.
| GP2GP_LOGGING_FORMAT               | (*)                       | Defines how to format log events on stdout
| GP2GP_AMQP_BROKERS                 | amqp://localhost:5672     | Defines amqp broker on which GP2GP will use.
| GP2GP_AMQP_USERNAME                |                           | (Optional) username for the AMQP server
| GP2GP_AMQP_PASSWORD                |                           | (Optional) password for the AMQP server
| GP2GP_AMQP_MAX_REDELIVERIES        | 3                         | The number of times an message will be retried to be delivered to consumer. After exhausting all retires, it will be put on DLQ.<queue_name> dead letter queue
| GP2GP_MONGO_URI                    | mongodb://localhost:27017 | Whole Mongo database connection string. Has a priority over other Mongo variables.
| GP2GP_MONGO_DATABASE_NAME          | gp2gp                     | Mongo database name.
| GP2GP_MONGO_HOST                   | (*)                       | Mongo database host. Can be left blank if full connection string is provided.
| GP2GP_MONGO_USERNAME               | (*)                       | Mongo database username. Can be left blank if full connection string is provided.
| GP2GP_MONGO_PASSWORD               | (*)                       | Mongo database password. Can be left blank if full connection string is provided.
| GP2GP_MONGO_OPTIONS                | (*)                       | Mongodb URL encoded parameters for the connection string without a leading "?". Can be left blank if full connection string is provided.
| GP2GP_MONGO_AUTO_INDEX_CREATION    | true                      | (Optional) Should auto index for Mongo database be created.
| GP2GP_MONGO_TTL                    | P30D                      | (Optional) Time-to-live value for inbound and outbound state collection documents as an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
| GP2GP_COSMOS_DB_ENABLED            | false                     | (Optional) If true the adaptor will enable features and workarounds to support Azure Cosmos DB.


(*) GP2GP API is using logback (http://logback.qos.ch/) for logging configuration.
Default log format is defined in the built-in logback.xml (https://github.com/NHSDigital/summary-care-record-api/tree/master/docker/service/src/main/resources/logback.xml)
This value can be overriden using `GP2GP_LOGGING_FORMAT` environment variable.
Alternatively, an external `logback.xml` with much more customizations can be provided using `-Dlogback.configurationFile` JVM parameter.

## How to run service:
* Navigate to `docker`
* Run script: `start-local-environment.sh`
* Add environment variables in order to connect to Mongo database. They can be also added directly to `applicaion.yml` file. If `uri` field value is not provided, other Mongo database fields will be used to construct the connection string.

If gradle-wrapper.jar doesn't exist navigate to docker/service in terminal and run:
* If gradle isn't installed `brew install gradle`
* Update gradle `gradle wrapper`

If ran through IDE on local machine:
* Setup local Mongo database. Tutorial can be viewed here: https://docs.mongodb.com/manual/tutorial/install-mongodb-on-os-x/

## How to run unit tests:
* Navigate to `service`
* Run: `./gradlew test`

## How to run integration tests:
* Navigate to `docker`
* Run: `./start-local-environment`
* Navigate to `service`
* Run: `./gradlew integrationTest`

## How to run style check:
* Navigate to `service`
* Run: `./gradlew staticCodeAnalysis` 

## How to run all checks:
* Navigate to `docker`
* Run: `./start-local-environment`
* Navigate to `service`
* Run: `./gradlew check` 

### Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).