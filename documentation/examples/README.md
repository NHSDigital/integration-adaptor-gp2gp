# Example Message Flow for GP2GP Adaptor

## 1 - EHR Request

The adaptor receives the EHR Request message on the MHS inbound queue.

## 2 - GPC Access Structured

The adaptor requests the patient's structured records.

Create the certificate and key files using values provided by OpenTest before running this `request.sh`

`endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-ca certs copied into the same file

Generate a new Authorization token. The easiest way is to run a Postman request against
the public demonstrator and copy the header value it generates.

## 3 - GPC Find Documents

The adaptor finds all documents for the patient.

New requirement - we need to call an additional endpoint to find all documents.

## 4 - GPC Access Document

The adaptor downloads all documents for the patient.

Same instructions as for "2 - GPC Access Structured"

## 5 - RCMR_IN030000UK06

Clone the mhs repository `https://github.com/nhsconnect/integration-adaptor-mhs` and follow the test scripts instructions to run the adaptor on version `1.0.0`

Follow instructions in each request script to:
- add asid to requests
- send request

On recieving a RCMR_IN010000UK05 the adaptor sends this oubound message to the mhs.

## 6 - COPC_IN000001UK01

The adaptor sends this oubound message to the mhs

Same instructions as for "5 - RCMR_IN030000UK06"

## 7 - MCCI_IN010000UK13

The adaptor sends this oubound message to the mhs

No response is returned for this acknowledgement. 

Same instructions as for "5 - RCMR_IN030000UK06"
