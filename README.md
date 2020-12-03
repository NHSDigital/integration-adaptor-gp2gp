# integration-adaptor-gp2gp
National Integration Adaptor - GP2GP API

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
| GP2GP_AMQP_BROKERS                 | (*)                       | Defines amqp broker on which GP2GP will use.
| GP2GP_AMQP_USERNAME                | (*)                       | Defines username for broker.
| GP2GP_AMQP_PASSWORD                | (*)                       | Defines password for broker.



(*) GP2GP API is using logback (http://logback.qos.ch/) for logging configuration.
Default log format is defined in the built-in logback.xml (https://github.com/NHSDigital/summary-care-record-api/tree/master/docker/service/src/main/resources/logback.xml)
This value can be overriden using `GP2GP_LOGGING_FORMAT` environment variable.
Alternatively, an external `logback.xml` with much more customizations can be provided using `-Dlogback.configurationFile` JVM parameter.

## How to run service:
* Navigate to `docker/service`
* Add environment tag `export TAG=latest`
* Run script: `build-image.sh` (excute privileges might be required `chmod +x build-image.sh`)
* Navigate to `docker`
* Add environment tags for AMQP `export GP2GP_AMQP_BROKERS=amqp://activemq:5672`
* Run script: `start-local-environment.sh`

If gradle-wrapper.jar doesn't exist navigate to docker/service in terminal and run:
* If gradle isn't installed `brew install gradle`
* Update gradle `gradle wrapper` 

## How to run unit tests:
* Navigate to `service`
* Run: `./gradlew test`

## How to run integration tests:
* Navigate to `service`
* Run: `./gradlew integrationTest`

## How to run style check:
* Navigate to `service`
* Run: `./gradlew staticCodeAnalysis` 

## How to run all checks:
* Navigate to `service`
* Run: `./gradlew check` 

### Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).