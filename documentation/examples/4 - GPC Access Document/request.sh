#!/bin/bash

set -x

# Provider URL for OpenTest
PROVIDER_URL='https://messagingportal.opentest.hscic.gov.uk:19192/B82617/STU3/1/gpconnect/documents/Binary/07a6483f-732b-461e-86b6-edb665c45510'

# Set SSP_URL to empty string to disable SSP
#SSP_URL='https://proxy.opentest.hscic.gov.uk/'
SSP_URL=''

# NOTE: the Authorization header needs to be updated manually with a non-expired JWT token. Use the postman collection
# against the public demonstrator to generate one

curl --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key \
--location --request GET "${SSP_URL}${PROVIDER_URL}" \
--header 'Accept: application/fhir+json' \
--header 'Ssp-From: 200000000359' \
--header 'Ssp-To: 918999198738' \
--header 'Ssp-InteractionID: urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1' \
--header 'Ssp-TraceID: 33e01f03-313a-4f4c-a2d3-1099a1a6cf5f' \
--header 'Authorization: Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwOi8vZ3Bjb25uZWN0LXBvc3RtYW4tdXJsIiwic3ViIjoiMSIsImF1ZCI6Imh0dHBzOi8vb3JhbmdlLnRlc3RsYWIubmhzLnVrL0I4MjYxNy9TVFUzLzEvZ3Bjb25uZWN0L2ZoaXIiLCJleHAiOjE2MTA0NDgwMTQsImlhdCI6MTYxMDQ0NzcxNCwicmVhc29uX2Zvcl9yZXF1ZXN0IjoiZGlyZWN0Y2FyZSIsInJlcXVlc3RlZF9zY29wZSI6Im9yZ2FuaXphdGlvbi8qLnJlYWQiLCJyZXF1ZXN0aW5nX2RldmljZSI6eyJyZXNvdXJjZVR5cGUiOiJEZXZpY2UiLCJpZCI6IjEiLCJpZGVudGlmaWVyIjpbeyJzeXN0ZW0iOiJXZWIgSW50ZXJmYWNlIiwidmFsdWUiOiJQb3N0bWFuIGV4YW1wbGUgY29uc3VtZXIifV0sIm1vZGVsIjoiUG9zdG1hbiIsInZlcnNpb24iOiIxLjAifSwicmVxdWVzdGluZ19vcmdhbml6YXRpb24iOnsicmVzb3VyY2VUeXBlIjoiT3JnYW5pemF0aW9uIiwiaWRlbnRpZmllciI6W3sic3lzdGVtIjoiaHR0cHM6Ly9maGlyLm5ocy51ay9JZC9vZHMtb3JnYW5pemF0aW9uLWNvZGUiLCJ2YWx1ZSI6IkdQQzAwMSJ9XSwibmFtZSI6IlBvc3RtYW4gT3JnYW5pemF0aW9uIn0sInJlcXVlc3RpbmdfcHJhY3RpdGlvbmVyIjp7InJlc291cmNlVHlwZSI6IlByYWN0aXRpb25lciIsImlkIjoiMSIsImlkZW50aWZpZXIiOlt7InN5c3RlbSI6Imh0dHBzOi8vZmhpci5uaHMudWsvSWQvc2RzLXVzZXItaWQiLCJ2YWx1ZSI6IkcxMzU3OTEzNSJ9LHsic3lzdGVtIjoiaHR0cHM6Ly9maGlyLm5ocy51ay9JZC9zZHMtcm9sZS1wcm9maWxlLWlkIiwidmFsdWUiOiIxMTExMTExMTEifV0sIm5hbWUiOlt7ImZhbWlseSI6IkRlbW9uc3RyYXRvciIsImdpdmVuIjpbIkdQQ29ubmVjdCJdLCJwcmVmaXgiOlsiTXIiXX1dfX0.'
