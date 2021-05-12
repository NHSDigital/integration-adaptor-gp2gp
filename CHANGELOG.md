# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


## [0.1.2] - 2021-05-07

- Enivronment variables updated in corrisponsing vars files in docker for integration with GPCC 0.0.5 changes. (Update environment variables accordingly)
- All vunerabile dependencies fixed with exception to one that current has no fxied list below in known issues.

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

- Known to be a broken version due to missing environment variables needed for GPCC inegration.
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
