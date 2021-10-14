# PoC for running GP2GP VnP tests

This project shows how local environment can be set up to execute performance tests on GP2GP adator

## Setup

1. Download the ["A" application](https://github.com/fmtn/a) and put the jar into this directory
2. Run `./start_gp2gp_vnp_containers.sh`
This will load necessary env vars and run a docker-compose command to start `activemq`, `mongodb`, `mock-mhs-adaptor`, `gpcc-mock` containers in "detached" mode - script will return to bash when all containers are started.
3. Run `./run_gp2gp_vnp_test.sh`
This will send content of `9690937286.json` file to `inbound` queue that will trigger GP2GP patient transfer for NHS number `9690937286`.<br/>
Script will pause for 5 seconds letting GP2GP completing it's work.<br/>
In the end it will fetch all requests that GP2GP has sent to MHS mock, and look for the last message looking for an AA ACK code.</br>
All containers must warm up, so the script might fail on the first run.</br>
This script can be run multiple times as each time it generates a new random ConversationId.</br>
At this step you can run `docker ps` and `docker logs <container_id or container_name>` to view each container logs. Look for CorrelationId to bind logs from different containers.
4. Run `./stop_gp2gp_vnp_containers.sh` to stop all containers.