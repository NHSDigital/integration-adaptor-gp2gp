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
| -------------------------------------|---------------------------|-------------
| GP2GP_SERVER_PORT                    | 8080                      | The port on which the GP2GP Adapter will run.
| GP2GP_LOGGING_LEVEL                  | INFO                      | Application logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data.
| GP2GP_LOGGING_FORMAT                 | (2)                       | Defines how to format log events on stdout

### File Storage Configuration Options

The adaptor uses AWS S3 or Azure Storage Blob to stage translated GP2GP HL7 and ebXML documents.

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GP2GP_STORAGE_TYPE                   | LocalMock                 | The type of storage solution. One of: S3, Azure, LocalMock
| GP2GP_STORAGE_CONTAINER_NAME         |                           | The name of the Azure Storage container or Amazon S3 Bucket
| GP2GP_AZURE_STORAGE_CONNECTION_STRING|                           | The connection string for Azure Blob Storage. Leave undefined if type is not Azure.
| AWS_ACCESS_KEY_ID                    |                           | The access key for Amazon S3. Leave undefined if using an AWS instance role.
| AWS_SECRET_ACCESS_KEY                |                           | The secret access key for Amazon S3. Leave undefined if using an AWS instance role.
| AWS_REGION                           |                           | The region for Amazon S3. Leave undefined if using an AWS instance role.

### Message Broker Configuration Options

The adaptor requires an AMQP 1.0 compatible message broker to 1) receive inbound Spine messages via MHS adaptor and 2)
queue its own internal asynchronous tasks

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GP2GP_AMQP_BROKERS                   | amqp://localhost:5672     | A comma-separated list of URLs to AMQP brokers (1)
| GP2GP_AMQP_USERNAME                  |                           | (Optional) username for the AMQP server
| GP2GP_AMQP_PASSWORD                  |                           | (Optional) password for the AMQP server
| GP2GP_AMQP_MAX_REDELIVERIES          | 3                         | The number of times an message will be retried to be delivered to consumer. After exhausting all retires, it will be put on DLQ.<queue_name> dead letter queue
| GP2GP_MHS_INBOUND_QUEUE              | inbound                   | Name of the queue for MHS inbound
| GP2GP_MHS_OUTBOUND_URL               |                           | URL of the MHS Outbound Endpoint
| GP2GP_TASK_QUEUE                     | gp2gpTaskQueue            | Defines name of internal taskQueue.

### GP Connect API Configuration Options

The adaptor uses the GP Connect API to fetch patient records and documents.

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GP2GP_GPC_GET_URL                    |                           | The URL used for GP Connect requests.
| GP2GP_GPC_GET_STRUCTURED_ENDPOINT    |                           | The endpoiint for GP Connect Get Structured Access. 
| GP2GP_SPINE_CLIENT_CERT              | gp2gp                     | The content of the PEM-formatted client endpoint certificate
| GP2GP_SPINE_CLIENT_KEY               | gp2gp                     | The content of the PEM-formatted client private key
| GP2GP_SPINE_ROOT_CA_CERT             | gp2gp                     | The content of the PEM-formatted certificate of the issuing Root CA.
| GP2GP_SPINE_SUB_CA_CERT              | gp2gp                     | The content of the PEM-formatted certificate of the issuing Sub CA.

Configure these if you access the OpenTest or HSCN networks via an HTTP proxy. This is NOT the configuration for Spine
Secure Proxy (SSP).

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GP2GP_GPC_ENABLE_HTTP_PROXY          | false                     | Enable your environment requires you to access HSCN or OpenTest networks via an HTTP proxy
| GP2GP_GPC_HTTP_PROXY                 | gp2gp                     | HTTP proxy address
| GP2GP_GPC_HTTP_PROXY_PORT            | gp2gp                     | HTTP proxy port

### Database Configuration Options

The adaptor requires a Mongodb-compatible database to manage its internal state.

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| GP2GP_MONGO_URI                      | mongodb://localhost:27017 | Whole Mongo database connection string. Has a priority over other Mongo variables.
| GP2GP_MONGO_DATABASE_NAME            | gp2gp                     | The database name.
| GP2GP_MONGO_HOST                     |                           | The database host. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_PORT                     |                           | The database port. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_USERNAME                 |                           | The database username. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_PASSWORD                 |                           | Mongo database password. Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_OPTIONS                  |                           | Mongodb URL encoded parameters for the connection string without a leading "?". Leave undefined if GP2GP_MONGO_URI is used.
| GP2GP_MONGO_AUTO_INDEX_CREATION      | true                      | (Optional) Should auto index for Mongo database be created.
| GP2GP_MONGO_TTL                      | P7D                       | (Optional) Time-to-live value for inbound and outbound state collection documents as an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
| GP2GP_COSMOS_DB_ENABLED              | false                     | (Optional) If true the adaptor will enable features and workarounds to support Azure Cosmos DB.

(1) Active/Standby: The first broker in the list always used unless there is an error, in which case the other URLs 
will be used. At least one URL is required.

(2) GP2GP API is using logback (http://logback.qos.ch/) for logging configuration.
Default log format is defined in the built-in [logback.xml](service/src/main/resources/logback.xml)
This value can be overriden using `GP2GP_LOGGING_FORMAT` environment variable.
Alternatively, an external `logback.xml` with much more customizations can be provided using `-Dlogback.configurationFile` JVM parameter.

## How to run service:

### Copy a configuration example

We provide several example configurations:
* `vars.local.sh` to run the adaptor with mock services
* `vars.public.sh` to run the adaptor with the GP Connect public demonstrator
* `vars.opentest.sh` to run the adaptor with providers and responders in OpenTest

```bash
cd docker/
cp vars.local.sh vars.sh
```

### Using the helper script for Docker Compose

Run `./start-local-environment.sh`

You can also run the docker-compose commands directly.

### From your IDE or the command line

First start the adaptor dependencies:

```
    cd docker/
    docker-compose build activemq wiremock mock-mhs-adaptor
    docker-compose up -d activemq wiremock mongodb mock-mhs-adaptor
```

Change into the service directory `cd ../service`

Build the project in your IDE or run `./gradlew bootJar`

Run `uk.nhs.adaptors.gp2gp.Gp2gpApplication` in your IDE or `java -jar build/libs/gp2gp.jar`

### Using Envfile for IntelliJ

An easy way to override the default configuration is to use an EnvFile with the EnvFile IntelliJ plugin.

To override environment variables choose an example file e.g. 
(service/env.opentest.example.yml)[service/env.opentest.example.yml] and copy it to `service/env.yml`. Make your 
changes in this copy. 


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

You can set the adaptor's environment variables to test integrations with specific dependencies.

**Example: Run integration tests with AWS S3 in-the-loop**

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

## How to run e2e tests:

End-to-end (e2e) tests execute against an already running / deployed adaptor and its dependencies. You must run these 
yourself and configure the environment variables as needed. The tests do not automatically start any dependencies.

These tests publish messages to the MHS inbound queue and make assertions on the Mongo database. The tests must have 
access to both the AMQP message queue and the Mongo database.

* Navigate to `e2e-tests`
* Run: `./gradlew check`

or run from Docker:

```
docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml build
docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml up --exit-code-from gp2gp-e2e-tests
```

Environment variables with the same name/meaning as the application's control the e2e test target environment:

* GP2GP_AMQP_BROKERS
* GP2GP_AMQP_USERNAME
* GP2GP_AMQP_PASSWORD
* GP2GP_MONGO_URI
* GP2GP_MONGO_DATABASE_NAME
* GP2GP_MHS_INBOUND_QUEUE

## How to use WireMock

We provide mocks of external APIs (GPC, SDS) for local development and testing.

* Navigate to `docker`
* `docker-compose up wiremock`

The folder `docker/wiremock/stubs` describes the supported interactions.

## How to use Mock MHS Adaptor

We provide a mock MHS adaptor for local development and testing.

* Navigate to `docker`
* `docker-compose up mock-mhs-adaptor`

| Environment Variable                 | Default                   | Description
| -------------------------------------|---------------------------|-------------
| MOCK_MHS_SERVER_PORT                 | 8081                      | The port on which the mock MHS Adapter will run.
| MOCK_MHS_LOGGING_LEVEL               | INFO                      | Mock MHS logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data.

## Troubleshooting

### gradle-wrapper.jar doesn't exist

If gradle-wrapper.jar doesn't exist run in terminal:
* Install Gradle (MacOS) `brew install gradle`
* Update gradle `gradle wrapper`


### Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
