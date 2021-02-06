# Example Message Flow for GP2GP Adaptor

Note: `COPC_IN000001UK01` appears several times. This is a general-purpose interaction type used
for different types of messages.

## 1 - MHS Inbound - RCMR_IN01000UK05 - EHR Request

The adaptor receives the EHR Request message on the MHS inbound queue.

## 2 - GP Connect API - Access Structured

The adaptor requests the patient's structured records.

Create the certificate and key files using values provided by OpenTest before running this `request.sh`

`endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-ca certs copied into the same file

Generate a new Authorization token. The easiest way is to run a Postman request against
the public demonstrator and copy the header value it generates.

## 3- GP Connect API - Documents - Find patient

Lookup logical id of patient on Document FHIR server. Refer to official GPC Postman collection.

## 4 - GGP Connect API - Documents - Find patient's documents

Find all the patient's documents given their logical identifier. Refer to official GPC Postman collection.

## 5 - GP Connect API - Access Document

The adaptor downloads all documents for the patient.

Same instructions as for "2 - GPC Access Structured"

## 6 - MHS Outbound - RCMR_IN030000UK06 - EHR Extract

The adaptor sends the translated EhrExtract to requesting practice via the MHS adaptor.

Clone the mhs repository `https://github.com/nhsconnect/integration-adaptor-mhs`. Follow the test-scripts instructions 
to run the adaptor version `1.0.0`.

Follow instructions in each request script to:
- add asid to requests
- send request

On recieving a RCMR_IN010000UK05 the adaptor sends this oubound message to the mhs.

## 7 - MHS Inbound - COPC_IN000001UK01 - Continue

The adaptor receives this message on the inbound queue. It is a receipt that the receiving practice
has received and processed the EHR Extract. The adaptor may now begin sending large message fragments.

The adaptor sends this outbound message to the mhs

Same instructions as for "5 - RCMR_IN030000UK06"

## 8 - MHS Outbound - COPC_IN000001UK01 - Large Message Fragments

The adaptor sends none (small EHR, no documents) to many (large EHR, many documents) large
messaging fragments to complete the Ehr Extract.

## 9 - MHS Outbound - MCCI_IN10000UK13 - Adaptor Application Acknowledgement

**Positive** The adaptor sends the requesting system a positive acknowledgement that is has sent all messages.

**Negative** The adaptor sends the requesting system a negative acknowledgement to indicate that the extract was not successful.

## 10 - MHS Inbound - MCCI_IN010000UK13 - Requester Application Acknowledgement

**Positive** The requesting system sends the adaptor a positive acknowledgement to confirm that the extract was successful.

**Negative** The requesting system sends the adaptor a negative acknowledgement to indicate that the extract was not successful.
