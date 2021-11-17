# Running Mock services

## Running the wire mocks

1. For the wiremock (:8110)

   `./docker-compose up wiremock`

2. For the gpc-api-mock (:8112)

   `./docker-compose up gpc-api-mock`

3. For the gpcc-mock (:8113)

   `./docker-compose up gpcc-mock`



## Requests for gpc-api-mock and gpcc-mock

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

1. 9690937286 - Response contains 27 Document references
2. 9690937287 - Response contains 1 Document reference that is missing



### Get requests (*/STU3/1/gpconnect/documents/fhir/Binary/{{id}})

1. id's 1-27 contain binaries provided for performance testing
2. Missing - operation outcome record not found