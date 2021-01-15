#!/bin/bash

set -x

# curl -i --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key https://msg.opentest.hscic.gov.uk

# Provider URL for OpenTest
PROVIDER_URL='https://messagingportal.opentest.hscic.gov.uk:19192/B82617/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord'

# Set SSP_URL to empty string to disable SSP
#SSP_URL='https://proxy.opentest.hscic.gov.uk/'
SSP_URL=''

# NOTE: the Authorization header needs to be updated manually with a non-expired JWT token. Use the postman collection
# against the public demonstrator to generate one

curl --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key \
--location --request POST "${SSP_URL}${PROVIDER_URL}" \
--header 'Accept: application/fhir+json' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1' \
--header 'Ssp-TraceID: 06efbd0f-058f-43ec-aa4b-cd0dc76bd5b1' \
--header 'Content-Type: application/fhir+json' \
--header 'Authorization: Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwOi8vZ3Bjb25uZWN0LXBvc3RtYW4tdXJsIiwic3ViIjoiMSIsImF1ZCI6Imh0dHBzOi8vb3JhbmdlLnRlc3RsYWIubmhzLnVrL0I4MjYxNy9TVFUzLzEvZ3Bjb25uZWN0L2ZoaXIiLCJleHAiOjE2MTA0NDkwNzgsImlhdCI6MTYxMDQ0ODc3OCwicmVhc29uX2Zvcl9yZXF1ZXN0IjoiZGlyZWN0Y2FyZSIsInJlcXVlc3RlZF9zY29wZSI6InBhdGllbnQvKi5yZWFkIiwicmVxdWVzdGluZ19kZXZpY2UiOnsicmVzb3VyY2VUeXBlIjoiRGV2aWNlIiwiaWQiOiIxIiwiaWRlbnRpZmllciI6W3sic3lzdGVtIjoiV2ViIEludGVyZmFjZSIsInZhbHVlIjoiUG9zdG1hbiBleGFtcGxlIGNvbnN1bWVyIn1dLCJtb2RlbCI6IlBvc3RtYW4iLCJ2ZXJzaW9uIjoiMS4wIn0sInJlcXVlc3Rpbmdfb3JnYW5pemF0aW9uIjp7InJlc291cmNlVHlwZSI6Ik9yZ2FuaXphdGlvbiIsImlkZW50aWZpZXIiOlt7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvb2RzLW9yZ2FuaXphdGlvbi1jb2RlIiwidmFsdWUiOiJHUEMwMDEifV0sIm5hbWUiOiJQb3N0bWFuIE9yZ2FuaXphdGlvbiJ9LCJyZXF1ZXN0aW5nX3ByYWN0aXRpb25lciI6eyJyZXNvdXJjZVR5cGUiOiJQcmFjdGl0aW9uZXIiLCJpZCI6IjEiLCJpZGVudGlmaWVyIjpbeyJzeXN0ZW0iOiJodHRwczovL2ZoaXIubmhzLnVrL0lkL3Nkcy11c2VyLWlkIiwidmFsdWUiOiJHMTM1NzkxMzUifSx7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvc2RzLXJvbGUtcHJvZmlsZS1pZCIsInZhbHVlIjoiMTExMTExMTExIn1dLCJuYW1lIjpbeyJmYW1pbHkiOiJEZW1vbnN0cmF0b3IiLCJnaXZlbiI6WyJHUENvbm5lY3QiXSwicHJlZml4IjpbIk1yIl19XX19.' \
--data-raw '{
    "resourceType": "Parameters",
    "parameter": [
        {
            "name": "patientNHSNumber",
            "valueIdentifier": {
                "system": "https://fhir.nhs.uk/Id/nhs-number",
                "value": "9690937294"
            }
        },
        {
      "name": "includeAllergies",
      "part": [
        {
          "name": "includeResolvedAllergies",
          "valueBoolean": true
        }
      ]
    },
    {
      "name": "includeMedication"
    },
    {
      "name": "includeConsultations"
    },
    {
      "name": "includeProblems"
    },
    {
      "name": "includeImmunisations"
    },
    {
      "name": "includeUncategorisedData"
    },
    {
      "name": "includeInvestigations"
    },
    {
      "name": "includeReferrals"
    }
  ]
}'