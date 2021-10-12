# PoC for running GPCC VnP tests

This project shows how local environment can be set up to execute performance tests on GPCC adaptor

## Setup

1. Run `./start_gpcc_vnp_containers.sh`
This will load necessary env vars and run a docker-compose command to start `gpcc`, `sds-api-mock`, `gpc-api-mock` containers in "detached" mode - script will return to bash when all containers are started.
3. Run `./run_gpcc_vnp_test.sh`
This will send 2 requests to `GPCC adaptor`:
- migrate structured record
- get document
At this step you can run `docker ps` and `docker logs <container_id or container_name>` to view each container logs.
4. Run `./stop_gpcc_vnp_containers.sh` to stop all containers.
