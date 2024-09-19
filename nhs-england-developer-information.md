# Developer Information

> [!WARNING]
> The audience of this documentation is designed for NHS England developers.

## Requirements:

* JDK 21 - We develop the adaptor in Java with Spring Boot
* Docker - We release the adaptor using Docker images on [Dockerhub](https://hub.docker.com/repository/docker/nhsdev/nia-gp2gp-adaptor)

## How to operate the adaptor

The following sections describe how to run the adaptor for development and testing.

Refer to [OPERATING.md](./OPERATING.md) for how to operate the adaptor in a live environment.

## How to run service:

The following steps use Docker to provide mocks of adaptor dependencies and infrastructure for local testing and
development. These containers are not suitable for use in a deployed environment. You are responsible for providing
adequate infrastructure and connections to external APIs.

We publish releases of the GP2GP adaptor container image to [Docker Hub](https://hub.docker.com/r/nhsdev/nia-gp2gp-adaptor).

### Copy a configuration example

We provide several example configurations:
* `vars.local.sh` to run the adaptor with mock services
* `vars.public.sh` to run the adaptor with the GP Connect public demonstrator docker image
* `vars.opentest.sh` to run the adaptor with providers and responders in OpenTest

```bash
cd docker/
cp vars.local.tests.sh vars.sh
```

Full descriptions of each configuration option can be found within the [OPERATING guidance](OPERATING.md#configuration).

### Using the helper script for Docker Compose

For local environment to run against mocks:
```bash
./start-local-environment-local.sh
```

For local environment to run against gp demonstrator 1.6.0
```bash
./start-local-environment-public.sh
```

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

Most integration tests automatically start their external dependencies using [TestContainers](https://www.testcontainers.org/).
To disable this set the `DISABLE_TEST_CONTAINERS` environment variable to `true`.

If a Test fails with a connection error, you will need to boot up the Docker dependencies.
See "How to run service" section for details.

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

## How to run smoke tests

Smoke tests are provided to check basic connectivity and the required resources are up and running.

- Information on running the smoke tests can be found [here](./smoke-tests/README.md).

## How to use WireMock

We provide mocks of external APIs (GPC, SDS) for local development and testing.

* Navigate to `docker`
* `docker-compose up wiremock`

The folder `wiremock/stubs` and [README](wiremock/README.md) describe the supported interactions.

## How to use Mock MHS Adaptor

We provide a mock MHS adaptor for local development and testing.

* Navigate to `docker`
* `docker-compose up mock-mhs-adaptor`

| Environment Variable   | Default | Description                                                                                                                     |
|------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------|
| MOCK_MHS_SERVER_PORT   | 8081    | The port on which the mock MHS Adapter will run.                                                                                |                                                                                
| MOCK_MHS_LOGGING_LEVEL | INFO    | Mock MHS logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data. | 

### How to transform arbitrary json ASR payload files

This is an interoperability testing tool to transform arbitrary/ad-hoc json ASR payloads and access the outputs.

1. Navigate to the input folder and place all Json files to convert here.
   `integration-adaptor-gp2gp/transformJsonToXml/input/`

2. Navigate to the TransformJsonToXml.sh file and run that script to execute the testing tool.
   `integration-adaptor-gp2gp/transformJsonToXml/`
   ```shell script
   cd transformJsonToXml
   ./TransformJsonToXml.sh
    ```

3. The Converted .Xml files will be located in the output folder.
   `integration-adaptor-gp2gp/transformJsonToXml/output/`

## Troubleshooting

### "Invalid source release 17" error

When using IntelliJ this could be caused by either of the following:

- The [Project SDK] being set too low.
- The [Gradle JDK] being set too low.

[Project SDK]: https://www.jetbrains.com/help/idea/sdk.html#change-project-sdk
[Gradle JDK]: https://www.jetbrains.com/help/idea/gradle-jvm-selection.html#jvm_settings

## Releasing a new version to Docker Hub

First identify which is the most recent commit within GitHub which contains only changes which are marked as Done within Jira.
You can also review what commits have gone in by using the git log command or IDE.

Make a note of the most recent Release within GitHub, and identify what the next version number to use will be.

On the repository home page, navigate to **Releases** and make sure the latest release is on screen. Click on the `Draft a new release` button

To create a new release within GitHub, specify a tag version to use (e.g. 0.11), and the target being the latest commit using the options available.
Click on the `Generate release notes` button and this will list all the current changes from the recent commit.

Click "Publish Release" which will trigger a GitHub Actions job called "Push Docker Image", which will build and
push images to DockerHub.

Update the `CHANGELOG.md` file, moving the UNRELEASED entries into a line for the new release.
Raise a PR for your changes.


