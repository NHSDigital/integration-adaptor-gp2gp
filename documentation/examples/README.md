# Example Message Flow for GP2GP Adaptor

Note: `COPC_IN000001UK01` appears several times. This is a general-purpose interaction type used
for different types of messages.

Ensure you have the following commands available:

- python
- envsubst
- uuidgen

Copy `vars.example.sh` to `vars.sh` and populate with values for your endpoint.

For messages from inbound - you can use `inbound_to_xml.sh` to decode the base64 from rabbitmq and extract the XML message.

## 01_ehr_request_RCMR_IN010000UK13 - MHS Outbound - EHR Request

The adaptor receives the EHR Request message on the MHS inbound queue.

## 02_gpc_get_access_structured - GP Connect API - Access Structured

The adaptor requests the patient's structured records.

Create the certificate and key files using values provided by OpenTest before running this `request.sh`

`endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-ca certs copied into the same file

Generate a new Authorization token. The easiest way is to run a Postman request against
the public demonstrator and copy the header value it generates.

## 3 - GP Connect API - Documents - Find patient

TODO

## 4 - GP Connect API - Documents - Find patient's documents

TODO

## 05_gpc_get_access_document - GP Connect API - Access Document

The adaptor downloads all documents for the patient.

Same instructions as for "2 - GPC Access Structured"

## 06_ehr_extract_RCMR_IN030000UK06 - MHS Outbound - RCMR_IN030000UK06 - EHR Extract

The adaptor sends the translated EhrExtract to requesting practice via the MHS adaptor.

Clone the mhs repository `https://github.com/nhsconnect/integration-adaptor-mhs`. Follow the test-scripts instructions 
to run the adaptor version `1.0.0`.

Follow instructions in each request script to:
- add asid to requests
- send request

We expect the 

## 7 - MHS Inbound - COPC_IN000001UK01 - Continue

TODO

## 08_ehr_large_message_COPC_IN000001UK01 - MHS Outbound - COPC_IN000001UK01 - Large Message Fragments

The adaptor sends none (small EHR, no documents) to many (large EHR, many documents) large
messaging fragments to complete the Ehr Extract.

## 09_ehr_ack_MCCI_IN010000UK13 - MHS Outbound - MCCI_IN10000UK13 - Adaptor Application Acknowledgement

**Positive** The adaptor sends the requesting system a positive acknowledgement that is has sent all messages.

**Negative** The adaptor sends the requesting system a negative acknowledgement to indicate that the extract was not successful.

## 10 - MHS Inbound - MCCI_IN010000UK13 - Requester Application Acknowledgement

TODO

**Positive** The requesting system sends the adaptor a positive acknowledgement to confirm that the extract was successful.

**Negative** The requesting system sends the adaptor a negative acknowledgement to indicate that the extract was not successful.
