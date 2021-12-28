# Running Mock services

## Running the wire mocks

1. For the wiremock (:8110)

   `./docker-compose up wiremock`

2. For the integration-wiremock (:8111)

   `./docker-compose up integration-wiremock`

3. For the gpc-api-mock (:8112)

   `./docker-compose up gpc-api-mock`

4. For the gpcc-mock (:8113)

   `./docker-compose up gpcc-mock`



## Requests for integration-wiremock, gpc-api-mock and gpcc-mock

### Post requests (*/{{odsCode}}/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord)

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
3. 9388098431 - Response contains 32 Document references with valid attachment types (integration-wiremock)
4. 9388098432 - Response contains 1 Document reference with a placeholder attachment (integration-wiremock)
5. 9388098433 - Response contains 1 Document reference with an invalid attachment type (integration-wiremock)



### Get requests (*/STU3/1/gpconnect/documents/fhir/Binary/{{id}})

1. id's 1-27 contain binaries provided for performance testing (gpc-api-mock and gpcc-mock)
2. Missing - operation outcome record not found (gpc-api-mock and gpcc-mock)
3. 07a6483f-732b-461e-86b6-edb665c45510-\[1-32] - contains binaries for valid attachment types (integration-wiremock)
4. 07a6483f-732b-461e-86b6-edb665c45510-0 - contains binary for invalid attachment type (integration-wiremock)

#### Valid Attachment Type IDs

| # | Document ID | MIME Type | Extension | isLarge |
|---|---|---|---|---|
| 1 | 07a6483f-732b-461e-86b6-edb665c45510-1 | application/msword | .doc | No |
| 2 | 07a6483f-732b-461e-86b6-edb665c45510-2 | application/pdf | .pdf | No |
| 3 | 07a6483f-732b-461e-86b6-edb665c45510-3 | application/xml | .xml | No |
| 4 | 07a6483f-732b-461e-86b6-edb665c45510-4 | application/vnd.ms-excel.addin.macroEnabled.12 | .xlam | No |
| 5 | 07a6483f-732b-461e-86b6-edb665c45510-5 | application/vnd.ms-excel.sheet.binary.macroEnabled.12 | .xlsb | No |
| 6 | 07a6483f-732b-461e-86b6-edb665c45510-6 | application/vnd.ms-excel.sheet.macroEnabled.12 | .xlsm | No |
| 7 | 07a6483f-732b-461e-86b6-edb665c45510-7 | application/vnd.ms-excel.template.macroEnabled.12 | .xltm | No |
| 8 | 07a6483f-732b-461e-86b6-edb665c45510-8 | application/vnd.ms-powerpoint.presentation.macroEnabled.12 | .pptm | No |
| 9 | 07a6483f-732b-461e-86b6-edb665c45510-9 | application/vnd.ms-powerpoint.slideshow.macroEnabled.12 | .ppsm | No |
| 10 | 07a6483f-732b-461e-86b6-edb665c45510-10 | application/vnd.ms-powerpoint.template.macroEnabled.12 | .potm | No |
| 11 | 07a6483f-732b-461e-86b6-edb665c45510-11 | application/vnd.ms-word.document.macroEnabled.12 | .docm | No |
| 12 | 07a6483f-732b-461e-86b6-edb665c45510-12 | application/vnd.ms-word.template.macroEnabled.12 | .dotm | No |
| 13 | 7a6483f-732b-461e-86b6-edb665c45510-13 | application/vnd.openxmlformats-officedocument.presentationml.template | .potx | No |
| 14 | 7a6483f-732b-461e-86b6-edb665c45510-14 | application/vnd.openxmlformats-officedocument.presentationml.slideshow | .ppsx | No |
| 15 | 7a6483f-732b-461e-86b6-edb665c45510-15 | application/vnd.openxmlformats-officedocument.presentationml.presentation | .pptx | No |
| 16 | 7a6483f-732b-461e-86b6-edb665c45510-16 | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | .xlsx | No |
| 17 | 7a6483f-732b-461e-86b6-edb665c45510-17 | application/vnd.openxmlformats-officedocument.spreadsheetml.template | .xltx | No |
| 18 | 7a6483f-732b-461e-86b6-edb665c45510-18 | application/vnd.openxmlformats-officedocument.wordprocessingml.template | .dotx | No |
| 19 | 7a6483f-732b-461e-86b6-edb665c45510-19 | application/vnd.openxmlformats-officedocument.wordprocessingml.document | .docx | No |
| 20 | 7a6483f-732b-461e-86b6-edb665c45510-20 | audio/basic | .au | No |
| 21 | 7a6483f-732b-461e-86b6-edb665c45510-21 | audio/mpeg | .mp3 | No |
| 22 | 7a6483f-732b-461e-86b6-edb665c45510-22 | image/bmp | .bmp | No |
| 23 | 7a6483f-732b-461e-86b6-edb665c45510-23 | image/jpeg | .jpeg | No |
| 24 | 7a6483f-732b-461e-86b6-edb665c45510-24 | image/gif | .gif | No |
| 25 | 7a6483f-732b-461e-86b6-edb665c45510-25 | image/png | .png | No |
| 26 | 7a6483f-732b-461e-86b6-edb665c45510-26 | image/tiff | .tiff | No |
| 27 | 7a6483f-732b-461e-86b6-edb665c45510-27 | text/html | .html | No |
| 28 | 7a6483f-732b-461e-86b6-edb665c45510-28 | text/plain | .txt | No |
| 29 | 7a6483f-732b-461e-86b6-edb665c45510-29 | text/richtext | .rtf | No |
| 30 | 7a6483f-732b-461e-86b6-edb665c45510-30 | text/rtf | .rtf | No |
| 31 | 7a6483f-732b-461e-86b6-edb665c45510-31 | text/xml | .xml | No |
| 32 | 7a6483f-732b-461e-86b6-edb665c45510-32 | video/mpeg | .mpeg | No |



