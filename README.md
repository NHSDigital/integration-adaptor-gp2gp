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

| Environment Variable                 | Default                   | Description
| ------------------------------------|---------------------------|-------------
| GP2GP_SERVER_PORT                    | 8080                      | The port on which the SCR API will run.
| GP2GP_LOGGING_LEVEL                  | INFO                      | Application logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data.
| GP2GP_LOGGING_FORMAT                 | (2)                       | Defines how to format log events on stdout
| GP2GP_STORAGE_TYPE                   | LocalMock                 | The type of storage solution. One of: S3, Azure, LocalMock
| GP2GP_STORAGE_CONTAINER_NAME         |                           | The name of the Azure Storage container or Amazon S3 Bucket
| GP2GP_AZURE_STORAGE_CONNECTION_STRING|                           | The connection string for Azure Blob Storage. Leave undefined if type is not Azure.
| AWS_ACCESS_KEY_ID                    |                           | The access key for Amazon S3. Leave undefined if using an AWS instance role.
| AWS_SECRET_ACCESS_KEY                |                           | The secret access key for Amazon S3. Leave undefined if using an AWS instance role.
| AWS_REGION                           |                           | The region for Amazon S3. Leave undefined if using an AWS instance role.
| GP2GP_AMQP_BROKERS                   | amqp://localhost:5672     | A comma-separated list of URLs to AMQP brokers (1)
| GP2GP_AMQP_USERNAME                  |                           | (Optional) username for the AMQP server
| GP2GP_AMQP_PASSWORD                  |                           | (Optional) password for the AMQP server
| GP2GP_AMQP_MAX_REDELIVERIES          | 3                         | The number of times an message will be retried to be delivered to consumer. After exhausting all retires, it will be put on DLQ.<queue_name> dead letter queue
| GP2GP_MONGO_URI                      | mongodb://localhost:27017 | Whole Mongo database connection string. Has a priority over other Mongo variables.
| GP2GP_MONGO_DATABASE_NAME            | gp2gp                     | The database name.
| GP2GP_MONGO_HOST                     |                           | The database host. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_USERNAME                 |                           | The database username. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_PASSWORD                 |                           | Mongo database password. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_OPTIONS                  |                           | Mongodb URL encoded parameters for the connection string without a leading "?". Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_AUTO_INDEX_CREATION      | true                      | (Optional) Should auto index for Mongo database be created.
| GP2GP_MONGO_TTL                      | P30D                      | (Optional) Time-to-live value for inbound and outbound state collection documents as an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
| GP2GP_COSMOS_DB_ENABLED              | false                     | (Optional) If true the adaptor will enable features and workarounds to support Azure Cosmos DB.

(1) Active/Standby: The first broker in the list always used unless there is an error, in which case the other URLs 
will be used. At least one URL is required.

(2) GP2GP API is using logback (http://logback.qos.ch/) for logging configuration.
Default log format is defined in the built-in [logback.xml](service/src/main/resources/logback.xml)
This value can be overriden using `GP2GP_LOGGING_FORMAT` environment variable.
Alternatively, an external `logback.xml` with much more customizations can be provided using `-Dlogback.configurationFile` JVM parameter.

## How to run service:
* Run `./start-local-environment.sh`

If gradle-wrapper.jar doesn't exist run in terminal:
* If gradle isn't installed `brew install gradle`
* Update gradle `gradle wrapper`

If ran through IDE on local machine:
* Setup local Mongo database. Tutorial can be viewed here: https://docs.mongodb.com/manual/tutorial/install-mongodb-on-os-x/

## How to run wiremock:

The project includes mock interactions of external APIs (GPC, SDS) implemented in Wiremock for local development.

* Navigate to `docker`
* `docker compose up wiremock`

The folder `docker/wiremock/stubs` describes the supported interactions.

## How to run tests

**Warning**: Gradle uses a [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) to re-use compile and
test outputs for faster builds. To re-run passing tests without making any code changes you must first run 
`./gradlew clean` to clear the build cache. Otherwise, gradle uses the cached outputs from a previous test execution to 
pass the build.

You must run all gradle commands from the `service/` directory.

### How to run unit tests:

```shell script
./gradlew test
```

### How to run all checks:

```shell script
./gradlew check
```

### How to run integration tests:

```shell script
./gradlew integrationTest
```

Integration tests automatically start their external dependencies using [TestContainers](https://www.testcontainers.org/). 
To disable this set the `DISABLE_TEST_CONTAINERS` environment variable to `true`.

#### Example: Run integration tests with AWS S3 in-the-loop

Use environment variables to configure the tests to use:
* An actual S3 bucket
* ActiveMQ running locally in Docker
* MongoDB running locally in Docker

1. Start activemq and mongodb dependencies manually

    ```shell script
    cd docker
    docker-compose up -d activemq mongodb
    ```

2. Configure environment variables

    If you're NOT running the test from an AWS instance with an instance role to access the bucket then the variables 
    `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_REGION` must also be set.
    
    ```shell script
    export DISABLE_TEST_CONTAINERS=true
    export GP2GP_STORAGE_CONTAINER_NAME=your-s3-bucket-name
    ```

3. Run the integration tests

    ```shell script
    cd ../service
    ./gradlew cleanIntegrationTest integrationTest -i
    ```  

### Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
