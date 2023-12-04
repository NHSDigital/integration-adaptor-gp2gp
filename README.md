# integration-adaptor-gp2gp
National Integration Adaptor - [GP2GP Sending Adaptor](https://digital.nhs.uk/developer/api-catalogue/gp2gp/gp2gp---integration-adaptor)

The existing GP2GP solution uses a legacy messaging standard and infrastructure (HL7v3 and Spine TMS). Reliance 
on these standards going forward presents a significant barrier to successful GP2GP implementation by new suppliers, 
and perpetuation of these standards in the long term presents a risk to the continued operation of GP2GP across all 
suppliers.

A hybrid solution approach has been selected as the best option for GP2GP adoption by NMEs and transition by existing 
incumbent suppliers.

The "National Integration Adaptor - GP2GP" implements a GP2GP 2.2b producer using the supplier's existing GP Connect 
Provider implementation to extract the Electronic Health Record. Suppliers that have not already implemented a 
GP2GP 2.2b producer, or those wishing to decommission their existing producer, may deploy the GP2GP adaptor in its place.

## Table of contents

1. [Guidance for operating the adaptor as a New Market Entrant](OPERATING.md)
2. [Guidance on integrating with the adaptors APIs](#how-to-query-the-ehr-status-api)
3. [Guidance for developing the adaptor](developer-information.md)

## How to query the EHR Status API

An API is provided to query the status of any transfer to an incumbent.

Requests can be made to the following endpoint using the *Conversation ID (SSP-TraceID)* of the transfer:

```http request
    {location of gp2gp service}/ehr-status/{conversationId} [GET]
```

The response will contain the following fields:

### EhrStatus

| Field name          | Description                                                                                                     | Data type                                        | Possible values                                                                                                  | nullable |
|---------------------|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------|----------|
| originalRequestDate | The date and time of the original request                                                                       | ISO-8601                                         |                                                                                                                  | False    |
| migrationStatus     | The current state of the transfer, a status of COMPLETE_WITH_ISSUES is given if placeholder documents were sent | string / enum                                    | COMPLETE <br/><br/>COMPLETE_WITH_ISSUES <br/><br/> FAILED_NME <br/><br/> FAILED_INCUMBENT <br/><br/> IN_PROGRESS | False    |
| attachmentStatus    | An array of statuses for each document sent during the transfer                                                 | Array of **AttachmentStatus** (See below)        |                                                                                                                  | False    |
| migrationLog        | An array containing details of acknowledgments received during the transfer                                     | Array of **ReceivedAcknowledgement** (See below) |                                                                                                                  | False    |

<br/>

### Subtypes

#### AttachmentStatus

| Field name          | Description                                                                                                                                                                                                                                                                           | Data type                           | Possible values                                     | Nullable |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|-----------------------------------------------------|----------|
| identifier          | An array of identifiers taken from the `identifier` element of the GP Connect `DocumentReference`                                                                                                                                                                                     | Array of **Identifier** (see below) |                                                     | False    |  
| fileStatus          | The status of the document sent to the winning practice (can be used to identify if a placeholder document was sent)                                                                                                                                                                  | string / enum                       | PLACEHOLDER <br/><br/>ORIGINAL_FILE <br/><br/>ERROR | False    |  
| filename            | The filename sent to winning practice in the GP2GP message                                                                                                                                                                                                                            | string                              |                                                     | False    |   
| originalDescription | The description of the file as given by the `description` element of the GP Connect `DocumentReference` resource.<br/><br/> This is inserted into the GP2GP placeholder document as the original filename and suffix. So should be of the form *filename.suffix*, e.g. *referral.txt* | string                              |                                                     | True     |

<br/>

#### Identifier

| Field name | Description                                                               | Data type | Nullable |
|------------|---------------------------------------------------------------------------|-----------|----------|
| system     | The `system` element of the Gp Connect `DocumentReference.identifier`     | string    | False    |    
| value      | The `identifier` element of the Gp Connect `DocumentReference.identifier` | string    | False    |


<br/>

#### ReceivedAcknowledgement

| Field name         | Description                                                                                                | Data type                      | Nullable |
|--------------------|------------------------------------------------------------------------------------------------------------|--------------------------------|----------|
| received           | The date and time the acknowledgment was received                                                          | ISO-8601                       | False    |
| conversationClosed | If the acknowledgement ended the transfer, the date and time the conversation was closed can be found here | ISO-8601                       | True     |
| errors             | An array of errors which will be populated in the case of a negative / rejected acknowledgement            | Array of **Error** (see below) | True     |      
| messageRef         | the Message ID of the message that has been acknowledged                                                   | string                         | False    |   

<br/>

#### Error

| Field name | Description                                        | Data type | Nullable |
|------------|----------------------------------------------------|-----------|----------|
| code       | The GP2GP response code from the negative response | string    | False    |
| display    | The GP2GP response text from the negative response | string    | False    | 

<details>
    <summary>EHR Status example responses</summary>

#### Successful migration, with single positive acknowledgement of EHR Extract (conversation closed and no errors):
```json

{
    "attachmentStatus": [],
    "migrationLog": [
        {
            "received": "2023-07-24T09:39:09.377Z",
            "conversationClosed": "2023-07-24T09:39:09.377Z",
            "errors": null,
            "messageRef": "0BEBCA12-8BE4-44B4-BDC0-016A4FE3D107"
        }
    ],
    "migrationStatus": "COMPLETE",
    "originalRequestDate": "2023-07-24T09:38:50.947Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```

#### Failed by requester, with single negative acknowledgement of EHR Extract (conversation closed and error):

```json

{
    "attachmentStatus": [],
    "migrationLog": [
        {
            "received": "2023-07-21T16:09:19.594Z",
            "conversationClosed": "2023-07-21T16:09:19.594Z",
            "errors": [
                {
                    "code": "11",
                    "display": "Failed to successfully integrate EHR Extract."
                }
            ],
            "messageRef": "C5271147-3D89-4EF6-A719-01AEB3AD00A1"
        }
    ],
    "migrationStatus": "FAILED_INCUMBENT",
    "originalRequestDate": "2023-07-21T16:09:12.695Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```


#### Failed by requester, with multiple positive acknowledgements for COPC messages (without conversation closed) and one negative acknowledgement for EHR Extract (conversation closed and error):

```json

{
    "attachmentStatus": [
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "ad174c84-51d1-4744-89e3-7918a31248d1"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "54E94A29-B0CC-4CD6-86B0-B21C9C0CA894.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        },
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "D7AF52BA-79BA-4AF8-9010-F0C2DF916CEC"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "629BF3F7-C71F-49D7-8FCD-AEE16C11AFBD.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        },
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "D7AF52BA-79BA-4AF8-9010-F0C2DF916CEC"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "2F73A243-0F94-4683-B3F9-727CE7780815.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        }
    ],
    "migrationLog": [
        {
            "received": "2023-07-24T10:42:18.591Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "ED2FB0DD-52AE-4EBC-B596-3BCA9CF5B272"
        },
                {
            "received": "2023-07-24T10:42:19.176Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "5068BAE1-7228-4E59-B3AD-92FA5CBDE4AC"
        },
                {
            "received": "2023-07-24T10:42:19.467Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "A9E37B68-07F4-43A5-889E-645D6681CE68"
        },
        {
            "received": "2023-07-24T10:42:22.611Z",
            "conversationClosed": "2023-07-24T10:42:22.611Z",
            "errors": [
                {
                    "code": "99",
                    "display": "Unexpected condition."
                }
            ],
            "messageRef": "E600D1D4-D5BE-4D0D-8080-F7F6188B0548"
        }
    ],
    "migrationStatus": "FAILED_INCUMBENT",
    "originalRequestDate": "2023-07-24T10:42:05.757Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```
#### Failed by adaptor, due to a PATIENT_NOT_FOUND error from the GP Connect provider's migrate structured endpoint
```json

{
    "attachmentStatus": [],
    "migrationLog": [],
    "migrationStatus": "FAILED_NME",
    "originalRequestDate": "2023-07-26T15:36:22.284Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```
</details>

## How to query the Requests endpoint

An API is provided to query the status of all transfers to an incumbent.
Any "in-progress" transfers are excluded from this list, but become available once they either succeed or fail.

Requests can be made to the following endpoint, where each attribute within the JSON POST body is an optional filter criteria.

```http request
POST /requests
Content-Type: application/json

{
    "fromDateTime": "2020-10-31T01:30:00.000Z",
    "toDateTime": "2030-10-31T01:30:00.000Z",
    "fromAsid": "",
    "toAsid": "",
    "fromOdsCode": "",
    "toOdsCode": ""
}
```

The response will contain a JSON array of the following:

### EhrStatusRequest
| Field name               | Description                                                                                                     | Data type     | Possible values                                                             | nullable |
| ------------------------ | --------------------------------------------------------------------------------------------------------------- | ------------- | --------------------------------------------------------------------------- | -------- |
| initialRequestTimestamp  | The date and time of the original request.                                                                      | ISO-8601      |                                                                             | False    |
| actionCompletedTimestamp | The date and time of when the transfer completed.                                                               | ISO-8601      |                                                                             | False    |
| nhsNumber                |                                                                                                                 | string        |                                                                             | False    |
| conversationId           |                                                                                                                 | string        |                                                                             | False    |
| fromAsid                 |                                                                                                                 | string        |                                                                             | False    |
| toAsid                   |                                                                                                                 | string        |                                                                             | False    |
| fromOdsCode              |                                                                                                                 | string        |                                                                             | False    |
| toOdsCode                |                                                                                                                 | string        |                                                                             | False    |
| migrationStatus          | The current state of the transfer, a status of COMPLETE_WITH_ISSUES is given if placeholder documents were sent | string / enum | COMPLETE <br/> COMPLETE_WITH_ISSUES <br/> FAILED_NME <br/> FAILED_INCUMBENT | False    |


#### Example EhrStatusRequest response

```json
[
    {
        "initialRequestTimestamp":"2023-09-20T11:47:58.966Z",
        "actionCompletedTimestamp":"2023-09-20T11:54:19.552Z",
        "nhsNumber":"9729734925",
        "conversationId":"59B118DB-70C3-4883-8A60-5E725981F003",
        "migrationStatus":"COMPLETE",
        "fromAsid":"858000001001",
        "toAsid":"200000001908",
        "fromOdsCode":"C88046",
        "toOdsCode":"B84012"
    },
    {
        "initialRequestTimestamp":"2023-09-20T12:18:10.364Z",
        "actionCompletedTimestamp":"2023-09-20T12:18:13.923Z",
        "nhsNumber":"9729735336",
        "conversationId":"C2A59970-57AF-11EE-AFE6-CD607DC58E3B",
        "migrationStatus":"FAILED_INCUMBENT",
        "fromAsid":"200000000169",
        "toAsid":"200000001908",
        "fromOdsCode":"P84009",
        "toOdsCode":"B84012"
    }
]
```

## Disclaimer

All Patient data within this repository is synthetic 

## Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
