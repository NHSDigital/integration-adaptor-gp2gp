# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.3] - 2021-02-19

### Known Issues and Limitations

- Same as for 0.0.2

### Added

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