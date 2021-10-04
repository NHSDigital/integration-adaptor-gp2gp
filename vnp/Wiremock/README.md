# Wiremock capabilities required for GP2GP and GPC Adaptor VnP testing

## Setup

### Random Binary resource content size

Response templating can be used to generate random strings in FHIR Binary resource content field.

Example can be found at [vnp/Wiremock/stubs/__files/binary.json](./stubs/__files/binary.json)

More information can be found [here](https://wiremock.org/docs/response-templating/)

### Response delay

The example mapping found at [vnp/Wiremock/stubs/mappings/test.json](./stubs/mappings/test.json), response delay has been set to a const 3000ms.

Delay can also be set as global Wiremock configuration and changed even while Wiremock is running using `POST /__admin/settings` request

More information can be found [here](http://wiremock.org/docs/simulating-faults/)

## Testing

Navigate to [vnp/Wiremock/](./) and start provided Wiremock container using:

    docker-compose rm -f; docker-compose build; docker-compose up;

Send GET request to Wiremock:

    curl http://localhost:8080/test

The response gets back after the configured 3000ms delay elapses.

Response body contains a FHIR Binary resource with `content` field filled with random 500 characters.