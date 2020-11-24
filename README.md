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
| GP2GP_SERVER_PORT                  | 8080                      | The port on which the SCR API will run
| GP2GP_LOGGING_LEVEL                | INFO                      | Application logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data.
| GP2GP_LOGGING_FORMAT               | (*)                       | Defines how to format log events on stdout


(*) SCR API is using logback (http://logback.qos.ch/) for logging configuration.
Default log format is defined in the built-in logback.xml (https://github.com/NHSDigital/summary-care-record-api/tree/master/docker/service/src/main/resources/logback.xml)
This value can be overriden using `SCR_LOGGING_FORMAT` environment variable.
Alternatively, an external `logback.xml` with much more customizations can be provided using `-Dlogback.configurationFile` JVM parameter.

## How to run service:
TODO
<!-- * Navigate to `ddocker/service`
* Add environment tag `export TAG=latest`
* Run script: `build-image.sh` (excute privileges might be required `chmod +x build-image.sh`)
* Navigate to `docker`
* Run script: `start-local-environment.sh`

If gradle-wrapper.jar doesn't exist navigate to docker/service in terminal and run:
* If gradle isn't installed `brew install gradle`
* Update gradle `gradle wrapper` -->

## How to run unit tests:
TODO
<!-- * Navigate to `service`
* Run: `./gradlew test` -->

## How to run integration tests:
TODO
<!-- * Navigate to `service`
* Run: `./gradlew integrationTest` -->

## How to run all checks:
TODO
<!-- * Navigate to `service`
* Run: `./gradlew check` -->
