# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
