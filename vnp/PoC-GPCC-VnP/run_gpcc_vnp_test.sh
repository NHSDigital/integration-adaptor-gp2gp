echo "### Sending 'get structured record' request to GPCC adaptor"
echo ""

curl --location --request POST 'http://localhost:8090/B86041/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1' \
--header 'Ssp-TraceID: 5fefd21d-17dd-4009-b595-0b9d953a286f' \
--header 'Authorization: Bearer some_token' \
--header 'Content-Type: application/fhir+json' \
--data-raw '{
    "resourceType": "Parameters",
    "parameter": [
        {
            "name": "patientNHSNumber",
            "valueIdentifier": {
                "system": "https://fhir.nhs.uk/Id/nhs-number",
                "value": "9690937286"
            }
        },
        {
            "name": "includeFullRecord",
            "part": [
                {
                    "name": "includeSensitiveInformation",
                    "valueBoolean": true
                }
            ]
        }
    ]
}'

echo ""
echo "### Done."

sleep 1

echo "### Sending 'get document' request to GPCC adaptor"
echo ""

curl --location --request GET 'http://localhost:8090/B82617/STU3/1/gpconnect/documents/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:documents:fhir:rest:migrate:binary-1' \
--header 'Ssp-TraceID: 5fefd21d-17dd-4009-b595-0b9d953a286f' \
--header 'Authorization: Bearer some_token'

echo ""
echo "### Done"