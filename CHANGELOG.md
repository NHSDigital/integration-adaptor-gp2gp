# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.1] - 2024-12-10

### Added
* When a transfer fails, the transfer should remain available in the db for at least 12 weeks (84 days), as per spec.
* When mapping a `DocumentReference` which contains a `NOPAT` `meta.security` or `NOPAT` `securityLabel` tag the resultant XML for that resource
  will contain a `NOPAT` `confidentialityCode` element.
* When mapping `AllergyIntolerances` which contain a `NOPAT` `meta.security` tag the resultant XML for that resource
  will contain a `NOPAT` `confidentialityCode` element.
* When mapping a `DiagnosticReport` or `Specimen` which contains a `NOPAT` `meta.security` tag the resultant XML for that resource
  will contain a `NOPAT` `confidentialityCode` element.
* When mapping a `TestResult`, `TestGroupHeader` or `FilingComment` which contains a `NOPAT` `meta.security` tag the resultant XML
  for that resource will contain a `NOPAT` `confidentialityCode` element.
* When mapping a `Condition` which contains a `NOPAT` `meta.security` tag the resultant XML for that resource
  will contain a `NOPAT` `confidentialityCode` element.
* When mapping a `MedicationRequest (PLAN)` which contains a `NOPAT` `meta.security` tag the resultant XML for that
  resource will contain a `NOPAT` `confidentialityCode` element.
* When mapping a `MedicationRequest (ORDER)` which contains a `NOPAT` `meta.security` tag the resultant XML for that
  resource will contain a `NOPAT` `confidentialityCode` element.
* When mapping `Immunizations` which contain a `NOPAT` `meta.security` tag, the resultant XML for that resource
  will contain a `NOPAT` `confidentialityCode` element.

## [2.2.0] - 2024-12-02

### Added
* New endpoint added `POST /ehr-resend/<conversationId>` which will re-request the GP Connect structured record, and
  resend a newly generated EHRExtract to the requesting GP2GP system.
  This endpoint can be used when the requesting system asks that the sending system resends the medical record
  because of a temporary technical fault.
  The endpoint will only perform resends if the status of the transfer is `FAILED_INCUMBENT` or `FAILED_NME` including
  when a transfer hasn't been acknowledged by the requesting system for 8 days or more.

### Fixed
* When mapping a `DiagnosticReport` which contains at least one test result with a `Specimen` attached,
  any test result's which didn't have a Specimen were previously not sent to the requesting system.
  Now, a fake `Specimen` is created in which any `Specimen`-less `TestResult`s are placed.

## [2.1.4] - 2024-11-07

### Fixed

* When mapping an `Observation` related to a diagnostic report which does not contain a `code` element, the adaptor will
  now throw an error reporting that an observation requires a code element and provide the affected resource ID.
  Previously the adaptor was generating an invalid GP2GP payload which was being rejected by the requesting system with
  a vague error code.
* When mapping a `valueQuantity` contained in an `Observation`, the generated XML element now correctly escapes any
  contained XML characters.
* Removed a 20 MB data processing limit which was causing large document transfers to fail.

## [2.1.3] - 2024-10-25

### Fixed

* Fix a malformed XML GP2GP message created when mapping a `MedicationRequest` with `intent` of `plan` and is stopped,
  but no free text reason for the discontinuation was provided.  

## [2.1.2] - 2024-10-21

### Fixed

* When mapping a `DiagnosticReport` which didn't contain any `Specimen` references, the adaptor would
  previously throw an error "EhrMapperException: Observation/ref was not mapped to a statement in the EHR" when mapping
  a filing comment and abort the GP2GP transfer.
  The adaptor is now able to handle this situation correctly.

## [2.1.1] - 2024-10-15

### Fixed

* When mapping a `Condition` without an `asserter`, omit the `Participant` element within the XML.
  Previously this would raise the error "EhrMapperException: Condition.asserter is required" and send a
  failure to the requesting system.
* When mapping a `MedicationRequest` without a `recorder` or `requester`, omit the `Participant` element within the XML.
  Previously this would raise the error "MedicationRequest ... missing recorder of type Practitioner, PractitionerRole
  or Organization" and send a failure to the requesting system.

## [2.1.0] - 2024-10-14

### Added

* Added functionality to automatically update the migration status to `FAILED_INCUMBENT` for any transfer where the 
  adaptor hasn't received an acknowledgement from the requesting GP surgery and the health record was sent to them more
  than 8 days ago.

### Fixed

* When mapping consultations which are "flat" (i.e., they contain a `TOPIC` without a `CATEGORY`) we now wrap the
  resource into a virtual `CATEGORY`.
  This provides compatability with a GP2GP system which would reject the transfer otherwise.
* When mapping an `Encounter` without a Recorder `participant`, now send the author as `nulFlavor=UNK`.
  Previously this would raise the error "EhrMapperException: Encounter.participant recorder is required" and send a
  failure to the requesting system.
* When mapping `ProcedureRequests` with an `occurrencePeriod` which contains only a start date, then the `text` element
  in the resultant XML will no longer contain the superfluous 'Earliest Recall Date: <startDate>' value.
* When mapping `ProcedureRequests` with a `requestor` referencing a `device` without a `manufacturer` a spurious 
  `"null"` is no longer output in the generated `"Recall Device:"` text.

## [2.0.6] - 2024-07-29

### Added

* When mapping `ReferralRequest` supporting info and `DiagnosticReports` NHS PMIP Report Numbers can also be provided 
with the code system as a URN (`urn:oid:2.16.840.1.113883.2.1.4.5.5`).

### Fixed
* When mapping physical quantities ("PQ") the produced XML now matches the HL7 XML specifications.

## [2.0.5] - 2024-07-04

### Fixed

* When mapping resources, if a UUID identifier is provided, this will be preserved in the produced XML.
  If a non-UUID identifier is provided, a new UUID will continue to be generated.
  This change should ensure that IDs are preserved into the new system when the record is transferred.

## [2.0.4] - 2024-06-17

### Fixed

* The GPC Consumer Adaptor JWT token is now refreshed with every request to prevent expiration issues during retries and ensure continuous, uninterrupted access; previously, we were seeing the JWT expire when a request failed and retried.

## [2.0.3] - 2024-05-20

### Fixed

- Correctly send documents which can't be fetched over GP Connect as absent attachments.
  Previously these documents wouldn't have the correct "Content Type", or "Filename" sent according to GP2GP specification.
  The adaptor also now sends the GP Connect error detail to the requesting practice to help diagnose the issue.

## [2.0.2] - 2024-04-10

### Fixed

- Updated dependencies to keep adaptor secure.

## [2.0.1] - 2024-02-22

### Fixed
- When mapping an `AllergyIntolerance` to an `ObservationStatement`, both the `availabilityTime` and `effectiveTime`
  fields were previously mapped from the `onset` field and the `assertedDate` field was ignored.
  Now, the `effectiveTime` is populated with the `onset` field, and the `availabilityTime` is populated with the
  `assertedDate` field.

## [2.0.0] - 2024-02-19

**Breaking change**: This release creates a minimum version requirement of 1.0.0 for the [GP Connect Consumer Adaptor][gpcc-adaptor].

### Fixed
- Fix GpConnectException (ASID_CHECK_FAILED_MESSAGESENDER) being thrown, a correct ASID value is fetched in GP Connect Consumer Adapter 

## [1.5.14] - 2024-01-05

### Added 
- Add configurable timeout for requests to MHS or GPCC adaptors
- Add retries with configurable backoff for 5xx or timeout from MHS or GPCC adaptors 

## [1.5.13] - 2024-01-04

### Changed
- Some data structures have been replaced with more efficient concurrent version to avoid any potential side effects
- REST client buffer limit has been increased to 150 Mb

### Fixed
- Fix logging message that incorrectly stated a conversation was closed on receipt of a negative ack referencing an
  attachment

## [1.5.12] - 2023-11-29

### Changed 
- Previously, a NOT_AUTHORISED (401) response from [`/$gpc.migratestructuredrecord`][migrate-structured-record] would generate a NACK with code 19.
  This behaviour has now been removed and instead a response type of NO_RELATIONSHIP (403) will produce NACK with code 19.

[migrate-structured-record]: https://developer.nhs.uk/apis/gpconnect-1-6-0/accessrecord_structured_development_migrate_patient_record.html

### Fixed

- Guard against possible null pointer error in exception handler.
- Fix errors within the generation of compressed EHR Extracts (happens when the record becomes >5MB) which was causing
  SystmOne to reject the transfer.

## [1.5.11] - 2023-09-26

### Fixed

- GP2GP bug - Fixed plain text file attachment being transferred into TPP as Base64Encoded. (#523)

## [1.5.10] - 2023-08-30

### Added

- Add "/requests" endpoint which returns a history of transfers made. (#500)
- Support for running the GP2GP adaptor and PS adaptors against single MHS inbound adaptor. (#494)

### Fixed

- GP2GP bug - Patient not found error was returning Response Code "6" instead of "06". (#501)
- When a GP2GP transfer fails because the MHS Adaptor rejects the attachments, we now return a Response Code of 30
  previously this situation was unhandled, and defaulted to 99. (#502)
- Mapping bug - DiagnosticReport result comments were previously generated as a NarrativeStatement with PMIP with 
  comment type `AGGREGATE COMMENT SET`, they are now generated with `USER COMMENT` (#504).
- Mapping bug - Lists which referenced [contained elements] were treated as invalid, they should now work (#507).
- GP2GP bug - Attachments with a content type not supported by Spine were being sent to the MHS Adaptor
  with their original content type. Now they are sent with `application/octect-stream` to match the GP2GP Spec. (#506)

[contained elements]: https://build.fhir.org/references.html#contained

## [1.5.7] - 2022-11-25

### Added

- fromAsid and toAsid - EHR Status Endpoint now contains To/From Asid.

## [1.5.6] - 2022-11-15

### Fixed

- Mapping bug - Missing unit of measurement in MedicationStatement and Observation
- Mapping bug - Missing practitioner name when mapping unstructured name
- Mapping bug - Wrong SMOMED code added for MedicationStatement when prescribed elsewhere
- Failover - Modify queue handling so messages are retried when the database, MHS Adaptor or SDS are down

## [1.5.5] - 2022-10-17

### Added

- EHR Status endpoint to query the status of transfers
- Add missing MIME content types for documents

### Known Issues

- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268]
- Information Exposure [Low Severity][https://snyk.io/vuln/SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744]

## [1.5.2] - 2022-08-11

### Added
- Send more descriptive reason codes in negative acknowledgement messages  

### Fixed
- Bug fixes for received acknowledgment messages ending or not ending processing correctly   

### Known Issues and Limitations

- Information Disclosure [Low Severity][https://security.snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415]
- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268]
- Information Exposure [Low Severity][https://snyk.io/vuln/SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744]

## [1.5.0] - 2022-04-27

- Large messaging bug fixes. 
- Fixed vulnerable dependencies by updating to Spring Boot version 2.6.7.

### Known Issues and Limitations

- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268]
- Information Exposure [Low Severity][https://snyk.io/vuln/SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744]

## [1.3.2] - 2021-09-29

- Bug fixes

### Known Issues and Limitations

- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268]
- Denial of Service (DoS) [High Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1584063]
- Denial of Service (DoS) [High Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1584064]

### Added

- NIAD-1613: move to latest EMIS test records (#344)
- Testing SSP Proxy against wiremock (#334)

### Fixed
- NIAD-1557: SystmOne won't display responsibleParty agents with representedOrganisation/id code (#357)
- NIAD-1703: AllergyIntolerance: Performer should fall back to recorder in absence of asserter  (#360)
- NIAD-1699: Observation: NIAD-1476 has resulted in spurious 'Interpretation' label being generated in most Observation output (#353)
- NIAD-1592: Immunization: When site is processed into to text the term text is incorrect for synonyms (#349)
- NIAD-1719: BloodPressure: Exception when no effectivePeriod.start (#351)
- NIAD-1779: DiagnosticReport: interpretationCode is output in wrong place (#352)
- NIAD-1560: observation value ratio is not processed (#354)
- NIAD-1523: Translate ehrComposition Location (#346)
- NIAD-1699: Observation: (Testing fixes) NIAD-1476 has resulted in spurious 'Interpretation' label being generated in most Observation output  (#343)
- NIAD-1560: Observation: made comparator to be considered optional in valueRatio (#345)
- NIAD-1526: MedicationRequest - confusion from priorPrescription/predecessor linkages (#342)
- NIAD-1480: AllergyIntolerance: Resolved drug allergies not accessible in Bundle (#340)
- NIAD-1389: document task queue payloads (#330)
- NIAD-1701: Observation: effectivePeriod with start but no end causes exception (#339)
- NIAD-1375: Immunization: primarySource attribute not mapped to notes text (#341)
- NIAD-1561: Observation: referenceRange not placed in notes text if no valueQuantity present is not processed (#337)
- NIAD-1699: Observation: NIAD-1476 has resulted in spurious 'Interpretation' label being generated in most Observation output (#335)
- NIAD-1667: MedicationRequest: quantityText is not being processed (#336)
- NIAD-1559: effectivetime low not handled by emisweb (testing changes) (#338)
- NIAD-1559: EffectiveTime not handled by EmisWeb (#324)
- NIAD-1539: DiagnosticReport: Spurious indent on 1st line of all pathology text/comment NarrativeStatement (#329)
- NIAD-1593: Immunization: The userSelected code is not being selected for the site code (#326)
- NIAD-1544: DiagnosticReport: Use AGGREGATE COMMENT SET instead of LABORATORY RESULT DETAIL(E136) (#328)


## [1.3.1] - 2021-09-10

- Bug fixes

### Known Issues and Limitations

- Same as release 1.3.0

### Added

- NIAD-1235: Ensure all Java streams are closed to prevent file descriptor leaks (#313)

### Fixed

NIAD-1743: Added confidential scope to jwt template (#327)
NIAD-1594: Processing Immunization.doseQuantity into Notes/Text (#319)
NIAD-1598: Immunization: Improve rendering of vaccination protocols - title for seriesDoses (#325)
NIAD-1603: AllergyIntolerance: Handle asserter as relatedPerson (#322)
NIAD-1596: Immunization: only 1st reason given processed into notes text (#318)
NIAD-1608: Ignore QuestionnaireResponse resource (#312)
NIAD-1597: Immunization: only 1st reason not given processed into notes text (#320)

## [1.3.0] - 2021-09-06

- Large messaging implementation

### Known Issues and Limitations

- Same as release 1.2.0

### Added

- NIAD-1504: Separation of large ehrExtract and normal attachments (#316)
- NIAD-1716: compress ehr extract (#302)
- NIAD-1735: MHS Mock request journal (#314)
- NIAD-1107: Consistently handle unresolvable references in Mappings (#308)
- NIAD-1108: Consistently handle reference to unexpected resource (#307)
- NIAD-1714: To generate attachment description on GP2GP side (#300)
- NIAD-1175: (Testing Fixes) Mapping Change - Handle ReferralRequest.supportingInfo (#305)
- NIAD-1171: handle procedure request.supporting info as notes text (#304)
- NIAD-1171: handle procedure request.supporting info as notes text (#303)
- NIAD-1504: Split large EhrExtract into multiple fragments (#296)
- NIAD-1171: Handle ProcedureRequest.supportingInfo as Notes/text (#299)
- NIAD-1175: Mapping Change - Handle ReferralRequest.supportingInfo (#293)
- NIAD-1173: niad 1577 related problem and notes added to observation stmt text (#285)

### Fixed

- NIAD-1481: generate ReferenceRange even when source text is missing (#283)
- NIAD-1520: Ensure drug allergy degrade (#289)
- NIAD-1583: Unrecognised encounter codes should display the original term text (#295)
- NIAD-1527: medication request suppress issues with status stopped (#290)
- NIAD-1430: EhrComposition effectiveTimes cannot have only upper bounds (#294)
- NIAD-1578: Condition: Change to reduce duplication of problem condition - where Condition.extension(actualProblem) is transformed to ObservationStatement do not generate additional ObservationStatement (#301)
- NIAD-1530: MedicationRequest: Place discontinuation reason term text in ehrSupplyDiscontinue/pertinentInformation (#297)
- NIAD-1578: (Testing fixes) Condition: Change to reduce duplication of problem condition (#306)
- NIAD-1576: availabilityTime of condition should use onSetDateTime or else output a nullFlavor of UNK (#298)
- NIAD-1589: Immunization.extension(parentPresent) value not being processed into notes text (#315)
- NIAD-1591: Immunization.reportOrigin not processed into notes text (#317)

## [1.2.0] - 2021-08-06

- Work on GPC 1.6 uplift

### Known Issues and Limitations

- Same as version 1.1.2

### Added

- NIAD-1416: implement gpc 1.6.0 (#288)
- NIAD-925: Documentation update. Tests updated for better coverage (#291)
- NIAD-924: Splitting large documents into chunks (#271)


### Known Issues and Limitations

- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents
- https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268
- https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316638
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316639
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316640
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316641

### Fixed

- NIAD-1577 related problem and notes added to observation stmt text (#285)
- NIAD-1481: generate ReferenceRange even when source text is missing (#283)

## [1.1.2] - 2021-07-21

- Work on Clincial Mapping changes

### Known Issues and Limitations

- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents
- https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268
- https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316638
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316639
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316640
- https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-1316641

### Added

- NIAD-1169: Throw error on missing observation (#284)
- NIAD-1176: Include requesting org type info in MedicationStatement's text (#282)
- NIAD-1174: map ReferralRequest.priority to RequestStatement.priorityCode (#279)
- Niad 1170: mapping change handle multiple instances of procedurerequest reason code (#281)
- NIAD-1169: Mapping change Observation .valueSampleData .valueAttachment (#280)
- NIAD-1476: Observation interpretation.text not being processed. (#276)
- NIAD-1548: mock mhs send ack update e2e tests (#277)
- NIAD-1172: DiagnosticReport.status mapped to NarrativeStatement (#278)
- NIAD-1473: Change missing unit in quantity in observations to not be mapped to null but to empty (#275)
- NIAD-1054: Handle out of order and illogical messages (#262)
- NIAD-1166: create integration tests for the handling of inbound messages (#273)
- NIAD-1474: Fix drug allergy code. (#270)
- NIAD-1285: Update url to take configurable ODS code (#274)
- NIAD-1166: create integration tests for the handling of task messages (#272)
- NIAD-1285: Add additional test for another gpconnect provider (#268)
- NIAD-1166: Verify that message processing is aborted when message can't be read (#267)
- NIAD-1464: Use IdMapper when creating NarrativeStatement elements (#253)
- NIAD-1031: Make the names of files uploaded to storage (Local HashMap, AWS, Azure) unique, to prevent accidental collisions/overwriting (#265)
- NIAD-1449: Prevent the execution of JSON to XML transform tool during tests (#266)
- NIAD-846: Send NACK on error (#257)
- NIAD-1285: support multiple gp connect providers (#258)
- NIAD-1166: Fail the whole conversation when the processing of inbound message or task fails (#260)
- NIAD-1449: Dynamic test fixture to transform arbitrary json ASR payload files (#241)
- NIAD-845: Rename test, increase wait time in e2e test for execution stability (#254)
- NIAD-1189: Handle document attachment with unsupported content-type(#249)
- NIAD-1373: Modify UAT test to check that all agentRef/id refer to actual Agent/id (#247)
- NIAD-845: Additional send acknowledgement automated tests (#251)
- NIAD-1337: Add fromAsid override variable (#242)
- NIAD-845: Include "Unknown" originalText for practitioners (#250)
- NIAD-910: Fixes to NIAD-910 after testing. (#245)
- NIAD-1340: Agent directory based on actual agent references (#233)
- NIAD-1205: Send positive acknowledgement after all docs sent (#246)
- NIAD-1472: make patient outputs match schema (#248)
- NIAD-1464 Ensure standalone Observations are mapped after DiagnosticReports (#244)
- NIAD-845: Send EHR Extract Common Fragments (#238)
- NIAD-1470: If Diagnostic Report has no specimens, create a dummy/default specimen, and associate observations with that specimen. (#239)
- NIAD-1469: diagnostic report with no results (#236)
- NIAD-1337: Added nhs number override to agent directory mapper (#235)
- NIAD-1378: Ignore empty Encounter elements when mapping EHR extract (#234)
- NIAD-910: Fix issue where some derived observations were not being mapped properly (#229)
- Niad 1467: specimen narrative statement (#232)
- NIAD-1383: Add missing Participant element to PlanStatement in HL7 payload (#231)
- NIAD-1455: Suppress Condition related clinical content linkages to Encounter (#230)
- NIAD-1405: Add Mold and Guerra test patients + schema validation (#226)
- NIAD-1300: Minor change to remove PractionerRole reference from a unit test (#228)
- NIAD-1424: Remove assumption and add Mock MHS debug log messages (#197)
- NIAD-1454: Update participant2 typecode (#227)
- NIAD-1300: Update wiremock responses to match opentest (#220)
- NIAD-1445: Bumped version number within DOCKERHUB.md file we copy to Docker hub page (#225)
- NIAD-1407: Removed assumption of resource type (#224)

### Fixed


## [0.1.2] - 2021-05-07

- Environment variables updated in corresponding vars files in docker for integration with GPCC 0.0.5 changes. (Update environment variables accordingly)
- All vulnerable dependencies fixed with exception to one that currently has no fix listed below in known issues.

### Known Issues and Limitations

- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents
- https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268

### Added

- NIAD-1445: Bump version to 0.1.1 (#206)
- NIAD-1412: Populate bolierplate values in EHR UAT (#208)
- NIAD-1410: Handle unmapped resources with Mapper
- NIAD-910: Map diagnostic report to compound statement 

### Fixed

- NIAD-1431: Add priorMedicationRef to ehrSupplyDiscontinue to MedicationRequest (#212)
- NIAD-1397: Content link in related clinical (#213)
- NIAD-1359: AgentDirectory structure (#211)
- NIAD-1398: Participant2 bug in allergy intolerance
- NIAD-1366: Missing statusCode to RequestStatement (#196)
- NIAD-1369: Order of linkset output (#205)
- NIAD-1429: Missing statusCode to Condition LinkSet (#209)
- NIAD-1432: Missing agent to BloodPressure (#214)
- NIAD-1428: Remove contextConductionId from Allergy Structure (#207)
- NIAD-1425: Remove valueQuantity element from Blood pressure (#203)
- NIAD-1365: Element order requeststatement (#202)
- NIAD-1367: Correct AllergyIntolerance mapping output to match schema (#200)
- NIAD-1380: Encode referenceRange text for XML (#199)

## [0.1.1] - 2021-05-06

### Known Issues and Limitations

- Known to be a broken version due to missing environment variables needed for GPCC integration.
- Same as 0.1.0

### Added

- NIAD-875: Receive EHR final ACK/NACK (#177)
- NIAD-1096: Map Encounter participant to EhrComposition author (#156)
- NIAD-1287: Sending external attachments to MHS (#164)
- NIAD-1288: EMIS full test extracts (#162)
- NIAD-911: translation of non-consultation resources (#158)
- NIAD-1070: Documentation Updates (#173)
- NIAD-1056: generate file extension (#168)

### Fixed

- NIAD-1427: Remove @contextConductionInd from MedicationStatement components (#201)
- NIAD-1368: Correct order of generated NarrativeStatement elements (#198)
- NIAD-1361: Fix element order of observation statements (#193)
- NIAD-1312: Testing fixes for RequestStatement (#194)
- NIAD-1363: Fix order of Immunization ObservationStatement elements (#192)
- NIAD-1364: Fix order of MedicationStatement elements (#191)
- NIAD-1362: Fix order of PlanStatement elements (#190)
- Niad 1339: uppercase UUIDs (#188)
- NIAD-1360: Allergy Intolerance remove nested ehrComposition (#187)
- NIAD-1056: Restore e2e tests and mock MHS changes to support recent MHS changes (#184)
- NIAD-1268: Update opentest variables (#182)
- NIAD-979: Change empty effective date mapping in ehr folder with effective time tag to be required (#180)
- NIAD-1312: Rework RequestStatementMapper tests (#178)
- NIAD-911: Added additional tests for non consultation mapper (#179)
- NIAD-1056: add audio/x-au mime type support (#174)
- NIAD-979: ehr folder to have max effective time from all ehr compositions (#166)
- Added additional test for multiple recipients (#171)
- NIAD-1319: Additional Unit Tests for MedicationRequest Mapper (#172)
- NIAD-1096: Add practitioner/practitionerRole/organization recorder when mapping medication request to medication statement (#159)
- NIAD-1306: Generic handling of Observation component (#167)
enable ptl deployment (#170)
- NIAD-1318: Change Participant2 typeCode from RESP to PPRF (#169)
- NIAD-1096: Add PPRF (or REC) participant to participant2 when mapping encounters to ehr compositions (#160)
- NIAD-1312: Allow request statements without onbehalfof (#165)
- Adding transformers to Encounter Components (#163)

## [0.1.0] - 2021-04-07

### Known Issues and Limitations

- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents or acknowledgements.
- Incomplete / invalid EhrExtract message. The adaptor does not yet support the complete message standard.
- Only supports GP2GP transfers from a single organisation (ODS Code).
- Denial of Service (DoS) [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-NETMINIDEV-1078499] in net.minidev:json-smart@2.3
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1082238] in io.netty:netty-transport-native-epoll@4.1.53.Final
- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268] in io.netty:netty-handler@4.1.59.Final
- HTTP Request Smuggling [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1083991] in io.netty:netty-codec-http2@4.1.59.Final
- Man-in-the-Middle (MitM) [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-ORGMONGODB-1079241] in org.mongodb:mongodb-driver-sync@4.1.1

### Added

- Spine Directory Service (SDS) and Spine Security Proxy (SSP) support via usage of GP Connect Consumer Adaptor
- NIAD-1154: Fix fhir base and update readme
- NIAD-840: Send application acknowledgement when all message parts are sent (no documents) (#131)
- NIAD-1181: Create unit/component tests for EMIS test extracts (#132)
- NIAD 913: Medication Request to Medication Statement (#130)
- NIAD-1024: Generate agent directory (#139)
- NIAD-1021: Translate Observation.interpretation in ObservationStatement (#136)
- NIAD-1154: Add GPC-Consumer Configuration to Jenkinsfile (#133)
- NIAD-1153: GP2GP Adaptor uses GPCC Adaptor for GP Connect requests (#129)
- NIAD-1024: Generate agent directory (#127)
- NIAD-1113: Map Encounter type to Ehr Composition code (#125)
- NIAD-1178: Create /healthcheck Endpoint for Mock-MHS & Wiremock (#128)

### Fixed

- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268] in io.netty:netty-handler@4.1.59.Final
- [Issue 116](https://github.com/nhsconnect/integration-adaptor-gp2gp/issues/116) Spine SSL context used for outbound http calls


## [0.0.4] - 2021-03-10

### Known Issues and Limitations

- No SDS support. The adaptor does not use the configuration options for SDS.
- No Spine Secure Proxy support. The adapter makes direct requests to the GP Connect provider.
- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents or acknowledgements.
- Incomplete / invalid EhrExtract message. The adaptor does not yet support the complete message standard.
- Denial of Service (DoS) [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-NETMINIDEV-1078499] in net.minidev:json-smart@2.3
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1082238] in io.netty:netty-transport-native-epoll@4.1.53.Final
- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268] in io.netty:netty-handler@4.1.59.Final
- HTTP Request Smuggling [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1083991] in io.netty:netty-codec-http2@4.1.59.Final


### Added

- NIAD-980 Build MHS mock alongside GP2GP (#122)
- NIAD-1020/1087: Updates from testing feedback (#123)
- NIAD-1092: Fixing pertinent information ordering (#124)
- NIAD-1142: Translate Blood Pressure Codeable Concepts (#121)
- NIAD-822:Publish-gp2gp-wiremock-Image (#119)
- NIAD-903: Add missing blood pressure mapping unit tests (#120)
- NIAD-1087: translating stubbed codeable concept to cd (#115)
- NIAD-906 blood pressure mapping (#118)
- Enhanced component test to ensure ExrExtract is parsable XML (#117)
- Add TF deployment redirection based on branch (#113)
- change dateutil format plus fix allergy intollerence tests (#114)
- NIAD 914: Translating Allergy Intolerance to Allergy Structure (#112)
- NIAD-907: translating to agent person and represented organisation (#108)
- NIAD-1082: Date Time human readable (#111)
- NIAD-904: Topic and category lists in encounter mapping (#107)
- Feature/niad 1019 translate care connect quantity value (#110)
- Feature/niad 1019 translate care connect quantity value (#104)
- NIAD-908: organization to agent testing feedback (#109)
- NIAD-1060: FHIR to HL7 date and times (#92)
- NIAD-905: map condition to link set (#105)
- NIAD-908: Transform organization to agent resource (#102)
- NIAD-916 Translating Referral Request to Request Statement (#96)
- NIAD-905: map condition to link set (#97)
- NIAD-1095: Remove "structured" path element from opentest vars file (#103)


### Fixed
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1082235] in io.netty:netty-handler@4.1.58.Final
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1070799] in io.netty:netty-codec-http@4.1.58.Final
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1082234] in io.netty:netty-common@4.1.58.Final
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1082236] in io.netty:netty-transport@4.1.58.Final

## [0.0.3] - 2021-02-19

### Known Issues and Limitations

- Same as for 0.0.2

### Added

- NIAD-1073: Force specific nhs number for test with env var (#99)
- NIAD-917: Add ProcedureRequest to PlanStatement mapping (#81)
- NIAD-1040: output ehr composition for each encounter (#82)
- NIAD-821: SDS Client (#84)
- NIAD-915: Additional tests for immunization mapper (#90)

### Fixed

- [Issue 98](https://github.com/nhsconnect/integration-adaptor-gp2gp/issues/98) NIAD-1103 JWT exp/iat values should be integers
- [Issue 94](https://github.com/nhsconnect/integration-adaptor-gp2gp/issues/94) NIAD-1094 Wrong content type in request message from Gp2Gp to MHS outbound adapter 

## [0.0.2] - 2021-02-17

### Known Issues and Limitations

- No SDS support. The adaptor does not use the configuration options for SDS.
- No Spine Secure Proxy support. The adapter makes direct requests to the GP Connect provider.
- Incomplete GP2GP workflow. The adaptor only sends the EhrExtract message. It cannot yet send documents or acknowledgements.
- Incomplete / invalid EhrExtract message. The adaptor does not yet support the complete message standard.
- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268] in io.netty:netty-handler@4.1.58.Final (No upgrade or patch available)
- Information Disclosure [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1070799] in io.netty:netty-codec-http@4.1.58.Final caused by org.apache.qpid:qpid-jms-client:0.56.0

### Added

- NIAD-1076: Pipeline to run against wiremock (#86)
- NIAD-1014: Update Mock SDS API to latest API version (#83)
- NIAD-915: translating immunization to observation statement (#80)
- NIAD-912: encounter to ehr composition (#79)
- NIAD-920: Translating Uncategorised Observation to Observation Statement (#78)
- NIAD-1033: Generate ids in the context of message (#74)
- NIAD-874: Fix log message (#77)
- NIAD-932: Trust store is set for mongo connection. No E2E tests imple… (#71)
- NIAD-814: Find the patient's documents (#75)
- NIAD-919 Translating Narrative Comment Note to Narrative Statement Improvements  (#73)
- NIAD-874: EHR Receive Continue Reply  (#70)
- NIAD-919: Translating FHIR Narrative (Comment Note) Observation to Narrative Statement (#66)
- NIAD-988: Document reference task marks GPC completions (#69)

### Fixed

- [Issue 88](https://github.com/nhsconnect/integration-adaptor-gp2gp/issues/88) Missing MHS header wait-for-response on outbound messages

## [0.0.1] - 2021-02-04

### Known Issues and Limitations
- No SDS support. The adaptor can only be used with a single GP Connect provider specified by the configuration.
- No Spine Secure Proxy support. The adapter makes direct requests to the GP Connect provider.
- Incomplete GP2GP workflow. The adaptor only send the EhrExtract message. It cannot yet send documents or acknowledgements.
- Incomplete EhrExtract message. The adaptor does not yet support the complete message standard.
- The adaptor does not yet send an EhrExtract message for patients without documents.
- Improper Certificate Validation [Medium Severity][https://snyk.io/vuln/SNYK-JAVA-IONETTY-1042268] in io.netty:netty-handler@4.1.54.Final (No upgrade or patch available)

### Added
- NIAD-649	GP2GP POC
- NIAD-764	Create Jenkins pipeline
- NIAD-765	Define TTL index on EHR Extract State collection
- NIAD-754	Create initial Spring Boot project structure
- NIAD-796	Prereqs - external service mocks
- NIAD-797	GP2GP end-to-end test project structure
- NIAD-760	Connect GP2GP adaptor to AMQP message queue
- NIAD-759	Connect GP2GP adaptor to Object Storage (AWS S3 / Azure Storage Blob)
- NIAD-790	Mock GPC API - Get Access Documents
- NIAD-762	Connect GP2GP adaptor to required databases
- NIAD-804	Terraform: Deploy GP2GP adaptor to build1 cluster
- NIAD-809	Logging level and add common identifiers to logging format
- NIAD-776	Receive a new EHR Request
- NIAD-761	Create initial task queue implementation
- NIAD-789	Mock GPC API - Get Access Structured
- NIAD-805	Pipeline: Deploy GP2GP adaptor to build1 cluster
- NIAD-879	Namespace queue names in GP2GP terraform deployment
- NIAD-806	Connect build1 cluster to OpenTest
- NIAD-783	Mock SDS API - GP Connect Use Case
- NIAD-773	Mock MHS adaptor
- NIAD-832	End-to-end tests support AWS
- NIAD-816	Detect when all GPC tasks are completed
- NIAD-815	Fetch GPC Access Document
- NIAD-788	GPC Get Access Structured
- NIAD-953	Run adaptor against wiremock by default and public demonstrator in pipeline
- NIAD-817	Send a stub EHR Extract "Core"
- NIAD-769	Deploy MHS adaptor to build1 cluster
- NIAD-818	Translate GPC Documents to EHR fragments
- NIAD-921	Structural Transform
- NIAD-820	Proxy configuration using GPC API in OpenTest
- NIAD-981	Transmission and Control Act Wrappers
- NIAD-844	Send Translated EHR Extract Core Message
- NIAD-814	Find the patient's documents

### Fixed
- Information Disclosure [Medium Severity] https://snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415 in com.google.guava:guava@29.0-jre
- Information Disclosure [High Severity] https://snyk.io/vuln/SNYK-JAVA-ORGCODEHAUSGROOVY-1048694 in org.codehaus.groovy:groovy@2.5.13
- Comparison Using Wrong Factors [High Severity] https://snyk.io/vuln/SNYK-JAVA-ORGBOUNCYCASTLE-1052448 in org.bouncycastle:bcprov-jdk15on@1.65
- Information Exposure [Medium Severity] https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-1048292 in org.apache.tomcat.embed:tomcat-embed-core@9.0.39
- Information Disclosure (new) [Medium Severity] https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-1061939 in org.apache.tomcat.embed:tomcat-embed-core@9.0.39
- Denial of Service (DoS) [High Severity] https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONDATAFORMAT-1047329 in com.fasterxml.jackson.dataformat:jackson-dataformat-cbor@2.11.3
- Use [Official Images](https://docs.docker.com/docker-hub/official_images/) for adaptor's base image.

[gpcc-adaptor]: https://github.com/NHSDigital/integration-adaptor-gpc-consumer
