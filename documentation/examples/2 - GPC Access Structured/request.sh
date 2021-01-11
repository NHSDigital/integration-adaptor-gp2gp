#!/bin/bash

set -x

# curl -i --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key https://msg.opentest.hscic.gov.uk

# Set SSP_URL to empty string to disable SSP
#PROVIDER_URL='https://messagingportal.opentest.hscic.gov.uk:19192/B82617/STU3/1/structured/fhir/Patient/$gpc.getstructuredrecord'
# Updated URL
PROVIDER_URL='https://messagingportal.opentest.hscic.gov.uk:19192/B82617/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord'

#SSP_URL='https://proxy.opentest.hscic.gov.uk/'
SSP_URL=''

curl --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key \
--verbose \
--location --request POST "${SSP_URL}${PROVIDER_URL}" \
--header 'Accept: application/fhir+json' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1' \
--header 'Ssp-TraceID: 06efbd0f-058f-43ec-aa4b-cd0dc76bd5b1' \
--header 'Content-Type: application/fhir+json' \
--header 'Authorization: Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwOi8vZ3Bjb25uZWN0LXBvc3RtYW4tdXJsIiwic3ViIjoiMSIsImF1ZCI6Imh0dHBzOi8vbWVzc2FnaW5ncG9ydGFsLm9wZW50ZXN0LmhzY2ljLmdvdi51azoxOTE5Mi9CODI2MTcvU1RVMy8xL3N0cnVjdHVyZWQvZmhpciIsImV4cCI6MTYwOTM0NDcxMiwiaWF0IjoxNjA5MzQ0NDEyLCJyZWFzb25fZm9yX3JlcXVlc3QiOiJkaXJlY3RjYXJlIiwicmVxdWVzdGVkX3Njb3BlIjoicGF0aWVudC8qLnJlYWQiLCJyZXF1ZXN0aW5nX2RldmljZSI6eyJyZXNvdXJjZVR5cGUiOiJEZXZpY2UiLCJpZCI6IjEiLCJpZGVudGlmaWVyIjpbeyJzeXN0ZW0iOiJXZWIgSW50ZXJmYWNlIiwidmFsdWUiOiJQb3N0bWFuIGV4YW1wbGUgY29uc3VtZXIifV0sIm1vZGVsIjoiUG9zdG1hbiIsInZlcnNpb24iOiIxLjAifSwicmVxdWVzdGluZ19vcmdhbml6YXRpb24iOnsicmVzb3VyY2VUeXBlIjoiT3JnYW5pemF0aW9uIiwiaWRlbnRpZmllciI6W3sic3lzdGVtIjoiaHR0cHM6Ly9maGlyLm5ocy51ay9JZC9vZHMtb3JnYW5pemF0aW9uLWNvZGUiLCJ2YWx1ZSI6IkdQQzAwMSJ9XSwibmFtZSI6IlBvc3RtYW4gT3JnYW5pemF0aW9uIn0sInJlcXVlc3RpbmdfcHJhY3RpdGlvbmVyIjp7InJlc291cmNlVHlwZSI6IlByYWN0aXRpb25lciIsImlkIjoiMSIsImlkZW50aWZpZXIiOlt7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvc2RzLXVzZXItaWQiLCJ2YWx1ZSI6IkcxMzU3OTEzNSJ9LHsic3lzdGVtIjoiaHR0cHM6Ly9maGlyLm5ocy51ay9JZC9zZHMtcm9sZS1wcm9maWxlLWlkIiwidmFsdWUiOiIxMTExMTExMTEifV0sIm5hbWUiOlt7ImZhbWlseSI6IkRlbW9uc3RyYXRvciIsImdpdmVuIjpbIkdQQ29ubmVjdCJdLCJwcmVmaXgiOlsiTXIiXX1dfX0.' \
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