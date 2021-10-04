# Quick reference
- Maintained by: NHS Digital
- Where to get help: https://github.com/nhsconnect/integration-adaptor-gp2gp
- Where to file issues: https://github.com/nhsconnect/integration-adaptor-gp2gp/issues

# What is the GP2GP Adaptor?
* A pre-assured implementation of part of the GP2GP 2.2b patient transfer process
* Part of a foundation system supplier's infrastructure to:
** enable transfers of patients from the supplier's system to other practices with GP2GP 2.2b compliant GP systems
** where the supplier implements a GP Connect v1.5 provider

# How to use this image

To help you begin using the GP2GP Adaptor we provide shell scripts and Docker Compose configurations.

## Pre-requisites

To get running make sure you have an OpenTest environment setup. The project also includes mock dependencies for local
testing and development.

## Clone the repository

```bash
git clone https://github.com/nhsconnect/integration-adaptor-gp2gp.git
```

## Pull the latest changes and checkout the release tag

Every tagged container on Docker hub has a corresponding tag in the git repository. Checkout the tag of the release 
you are testing to ensure compatibility with configurations and scripts.

```bash
git pull
git checkout 1.3.2
```

## Find the docker directory

```bash
cd integration-adaptor-gp2gp/docker
```

## Configure the application

The repository includes several configuration examples:
* `vars.local.e2e.sh` or `vars.local.tests.sh` template to run the adaptor against mock service containers
* `vars.opentest.sh` template to run the adaptor against the OpenTest environment

Configure the application by copying a `vars.*.sh` file to `vars.sh`

```bash
cp vars.local.e2e.sh vars.sh
```

Make any required changes to the `vars.sh` file. If using `vars.local.sh` you do not need to modify anything. Refer
to the [README](https://github.com/nhsconnect/integration-adaptor-gp2gp/blob/0.1.0/README.md) for possible configuration
options.

## Find the release directory

```bash
cd ../release
```

## Start the adaptor

The script pulls the released GP2GP adaptor container image from Docker Hub. It builds containers for its dependencies
from the Dockerfiles in the repository.

```bash
./run.sh
```

## Monitor the logs

```bash
./logs.sh
```

## Run the tests

We provide shell scripts in the release/tests directory to help you start testing.

* `healthcheck.sh` verifies that the adaptor's healthcheck endpoint is available
* `e2e.sh` starts a docker container that runs the adaptor's end-to-end tests

```bash
cd tests/
./healthcheck.sh
./e2e.sh
```

## Stopping the adaptor
```bash
cd ../docker
docker-compose down
```
