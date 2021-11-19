# Running Mock services

## Running the wire mocks

1. For the wiremock (:8110)

   `./docker-compose up wiremock`

2. For the integration-wiremock (:8111)

   `./docker-compose up integration-wiremock`

3. For the gpc-api-mock (:8080)

   `./docker-compose up gpc-api-mock`

4. For the gpcc-mock (:8080)

   `./docker-compose up gpcc-mock`



## Requests for integration-wiremock, gpc-api-mock and gpcc-mock

### Post requests (*/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord)

```{
  "resourceType": "Parameters",

  "parameter": [

  {

​    "name": "patientNHSNumber",

​    "valueIdentifier": {

​        "system": "https://fhir.nhs.uk/Id/nhs-number",

​        "value": "{{nhsNumber}}"

​    }

  }

}
```

1. 9690937286 - Response contains 27 Document references (gpc-api-mock and gpcc-mock)
2. 9690937287 - Response contains 1 Document reference that is missing (gpc-api-mock and gpcc-mock)
3. 9388098431 - Response contains 10 Document references with valid attachment types (integration-wiremock)
4. 9388098432 - Response contains 1 Document reference with a placeholder attachment (integration-wiremock)
5. 9388098433 - Response contains 1 Document reference with an invalid attachment type (integration-wiremock)



### Get requests (*/STU3/1/gpconnect/documents/fhir/Binary/{{id}})

1. id's 1-27 contain binaries provided for performance testing (gpc-api-mock and gpcc-mock)
2. Missing - operation outcome record not found (gpc-api-mock and gpcc-mock)
3. 07a6483f-732b-461e-86b6-edb665c45510-\[1-10] - contains binaries for valid attachment types (integration-wiremock)
4. 07a6483f-732b-461e-86b6-edb665c45510-0 - contains binaries for invalid attachment type (integration-wiremock)