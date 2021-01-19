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

## 5 - EHR Request Completed


## 6 - EHR Extract Acknowledged (positive)


## 7 - EHR Extract Acknowledged (positive)