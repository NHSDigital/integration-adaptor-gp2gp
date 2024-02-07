# Wiremock

## Patients - `migratestructuredrecord` endpoint

### Invalid/error responses
- [OperationOutcome - Invalid NHS Number](stubs/__files/operationOutcomeInvalidNHSNumber.json) 123456789, 960000001
- [OperationOutcome - Bad Request](stubs/__files/operationOutcomeBadRequest.json) 9600000005
- [OperationOutcome - Internal Server Error](stubs/__files/operationOutcomeInternalServerError.json) 9600000006
- [OperationOutcome - Invalid demographics](stubs/__files/operationOutcomeInvalidDemographic.json) 9600000002
- [OperationOutcome - Invalid Parameter](stubs/__files/operationOutcomeInvalidParameter.json) 9600000004
- [OperationOutcome - Invalid Resource](stubs/__files/operationOutcomeInvalidResource.json) 9600000003
- [OperationOutcome - No Relationship](stubs/__files/operationOutcomeNoRelationship.json) 9600000010
- [Bundle without Patient Resource](stubs/__files/malformedStructuredRecordMissingPatientResource.json) 2906543841
- [OperationOutcome - Not Found](stubs/__files/operationOutcomePatientNotFound.json) 9600000009, also used as a fallback for any unrecognised NHS number.

### Valid bundles

- [Allergies](stubs/__files/correctAllergiesContainedResourceResponse.json) 9728951256
- [BundleFromMedicus](stubs/__files/MedicusBasedOnErrorStructuredRecord.json) 9302014592
- [Observation with bodySite property](stubs/__files/correctPatientStructuredRecordResponseBodySite.json) 1239577290
- [Large (3Mb) structured record Bundle](stubs/__files/correctPatientStructuredRecordLargePayload.json) 9690937421
- [Malformed date](stubs/__files/malformedDateStructuredRecord.json) 9690872294
- ["Normal"](stubs/__files/correctPatientStructuredRecordResponseNormal.json) 9690937287

#### Document scenarios

- [No Documents](stubs/__files/correctPatientNoDocsStructuredRecordResponse.json) 9690937294
- [1 Absent Attachment](stubs/__files/correctPatientStructuredRecordResponseAbsentAttachment.json) 9690937286
- [3 Absent Attachments](stubs/__files/correctPatientStructuredRecordResponse3AbsentAttachmentDocuments.json) 9690937419
- [With three 10Kb .doc files](stubs/__files/correctPatientStructuredRecordResponse3NormalDocuments.json) 9690937420
- [With one 20Kb document](stubs/__files/correctPatientStructuredRecordResponseForLargeDocs.json) 9690937819
- [With one 40Kb document](stubs/__files/correctPatientStructuredRecordResponseForLargeDocs2.json) 9690937841
- [With one 74Kb docx file](stubs/__files/correctPatientStructuredRecordWithLargeDocxAttachment.json) 9388098434
- [One 10Kb doc, one 4.703Kb doc](stubs/__files/correctPatientStructuredRecordResponseOneLargeDocOneNormal.json) 9690937789
- [With invalid content type document](stubs/__files/correctPatientStructuredRecordResponseOneInvalidContentTypeAttachment.json) 9817280691

### Assurance patients

- PWTP2 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP2.json) 9726908671 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP2.json) 9726908787
- PWTP3 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP3.json) 9726908698 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP3.json) 9726908795
- PWTP4 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP4.json) 9726908701 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP4.json) 9726908809
- PWTP5 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP5.json) 9726908728 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP5.json) 9726908817
- PWTP6 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP6.json) 9726908736 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP6.json) 9726908825
- PWTP7 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP7.json) 9726908744 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP7.json) 9726908833
- PWTP9 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP9.json) 9726908752 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP9.json) 9726908841
- PWTP10 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP10.json) 9726908760 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP10.json) 9726908868
- PWTP11 [EMIS](stubs/__files/EMISPatientStructurede2eResponsePWTP11.json) 9726908779 [TPP](stubs/__files/TPPPatientStructuredRecordE2EPWTP11.json) 9726908876