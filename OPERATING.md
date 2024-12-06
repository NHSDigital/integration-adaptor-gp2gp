# Operating

How to operate the GP2GP adaptor

## Configuration

The adaptor reads its configuration from environment variables. The following sections describe the environment variables
used to configure the adaptor.

Variables without a default value and not marked optional, *MUST* be defined for the adaptor to run.

### General Configuration Options

| Environment Variable     | Default                   | Description                                                                               |
|--------------------------|---------------------------|-------------------------------------------------------------------------------------------|
| GP2GP_SERVER_PORT        | 8080                      | The port on which the GP2GP Adapter API will run.                                         |
| GP2GP_ROOT_LOGGING_LEVEL | WARN                      | The logging level applied to the entire application (including third-party dependencies). |
| GP2GP_LOGGING_LEVEL      | INFO                      | The logging level applied to GP2GP adaptor components.                                    |
| GP2GP_LOGGING_FORMAT     | (*)                       | Defines how to format log events on stdout                                                |

Logging levels are ane of: DEBUG, INFO, WARN, ERROR

The level DEBUG **MUST NOT** be used when handling live patient data.

(*) GP2GP API uses logback (http://logback.qos.ch/). The built-in [logback.xml](service/src/main/resources/logback.xml)
defines the default log format. This value can be overridden using the `GP2GP_LOGGING_FORMAT` environment variable.
You can provide an external `logback.xml` file using the `-Dlogback.configurationFile` JVM parameter.

### Database Configuration Options

The adaptor requires a Mongodb-compatible database to manage its internal state.

| Environment Variable            | Default                   | Description                                                                                                                                                    |
|---------------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_MONGO_URI                 | mongodb://localhost:27017 | Whole Mongo database connection string. Has a priority over other Mongo variables.                                                                             |
| GP2GP_MONGO_DATABASE_NAME       | gp2gp                     | The database name.                                                                                                                                             |
| GP2GP_MONGO_HOST                |                           | The database host. Leave undefined if GP2GP_MONGO_URI is used.                                                                                                 |
| GP2GP_MONGO_PORT                |                           | The database port. Leave undefined if GP2GP_MONGO_URI is used.                                                                                                 |
| GP2GP_MONGO_USERNAME            |                           | The database username. Leave undefined if GP2GP_MONGO_URI is used.                                                                                             |
| GP2GP_MONGO_PASSWORD            |                           | Mongo database password. Leave undefined if GP2GP_MONGO_URI is used.                                                                                           |
| GP2GP_MONGO_OPTIONS             |                           | Mongodb URL encoded parameters for the connection string without a leading "?". Leave undefined if GP2GP_MONGO_URI is used.                                    |
| GP2GP_MONGO_AUTO_INDEX_CREATION | true                      | (Optional) Should auto index for Mongo database be created.                                                                                                    |
| GP2GP_MONGO_TTL                 | P84D                      | (Optional) Time-to-live value for inbound and outbound state collection documents as an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations). |
| GP2GP_COSMOS_DB_ENABLED         | false                     | (Optional) If true the adaptor will enable features and workarounds to support Azure Cosmos DB.                                                                |

**Trust Store Configuration Options**

You can configure a trust store with private CA certificates if required for TLS connections. The trust store does not
replace Java's default trust store. At runtime the application adds these additional certificates to the default trust
store. Only an s3:// url is currently supported, and the current use-case is to support AWS DocumentDb.

| Environment Variable           | Default | Description                                                                           |
|--------------------------------|---------|---------------------------------------------------------------------------------------|
| GP2GP_SSL_TRUST_STORE_URL      |         | (Optional) URL of the trust store JKS. The only scheme currently supported is `s3://` |
| GP2GP_SSL_TRUST_STORE_PASSWORD |         | (Optional) Password used to access the trust store                                    |

### File Storage Configuration Options

The adaptor uses AWS S3 or Azure Storage Blob to stage translated GP2GP HL7 and ebXML documents.

| Environment Variable                  | Default   | Description                                                                         |
|---------------------------------------|-----------|-------------------------------------------------------------------------------------|
| GP2GP_STORAGE_TYPE                    | LocalMock | The type of storage solution. One of: S3, Azure, LocalMock                          |
| GP2GP_STORAGE_CONTAINER_NAME          |           | The name of the Azure Storage container or Amazon S3 Bucket                         |
| GP2GP_AZURE_STORAGE_CONNECTION_STRING |           | The connection string for Azure Blob Storage. Leave undefined if type is not Azure. |
| AWS_ACCESS_KEY_ID                     |           | The access key for Amazon S3. Leave undefined if using an AWS instance role.        |
| AWS_SECRET_ACCESS_KEY                 |           | The secret access key for Amazon S3. Leave undefined if using an AWS instance role. |
| AWS_REGION                            |           | The region for Amazon S3. Leave undefined if using an AWS instance role.            |

### Message Broker Configuration Options

The adaptor requires an AMQP 1.0 compatible message broker to 1) receive inbound Spine messages via MHS adaptor and 2)
queue its own internal asynchronous tasks

| Environment Variable                  | Default               | Description                                                                                                                                                                                                       |
|---------------------------------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_AMQP_BROKERS                    | amqp://localhost:5672 | A comma-separated list of URLs to AMQP brokers (*)                                                                                                                                                                |
| GP2GP_AMQP_USERNAME                   |                       | (Optional) username for the AMQP server                                                                                                                                                                           |
| GP2GP_AMQP_PASSWORD                   |                       | (Optional) password for the AMQP server                                                                                                                                                                           |
| GP2GP_AMQP_MAX_REDELIVERIES           | 3                     | The number of times an message will be retried to be delivered to consumer. After exhausting all retires, it will be put on DLQ.<queue_name> dead letter queue                                                    |
| GP2GP_TASK_QUEUE                      | gp2gpTaskQueue        | Defines name of internal taskQueue.                                                                                                                                                                               |
| GP2GP_TASK_QUEUE_CONSUMER_CONCURRENCY | 1                     | Defines the number of concurrent task queue consumers in a single application. https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/annotation/JmsListener.html#concurrency-- |

(*) Active/Standby: The first broker in the list always used unless there is an error, in which case the other URLs
will be used. At least one URL is required.

### GP Connect API Configuration Options

The adaptor fetches patient records and documents with the GP Connect Consumer Adaptor
([Github](https://github.com/nhsconnect/integration-adaptor-gpc-consumer) /
[Dockerhub](https://hub.docker.com/repository/docker/nhsdev/nia-gpc-consumer-adaptor)) consuming the
[GP Connect API](https://developer.nhs.uk/apis/gpconnect/).

| Environment Variable           | Default                                           | Description                                                                                                                                            |
|--------------------------------|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_GPC_GET_URL              | http://localhost:8090/@ODS_CODE@/STU3/1/gpconnect | (*) The base URL of the GP Connect Consumer Adaptor. @ODS_CODE@ is a placeholder replaced in runtime with the actual ODS code of the loosing practice. |
| GP2GP_GPC_STRUCTURED_FHIR_BASE | /fhir                                             | The path segment for Get Access Structured FHIR server                                                                                                 |
| GP2GP_GPC_MAX_REQUEST_SIZE     | 150000000 (150 MB)                                | Buffer size when downloading data from GPC                                                                                                             |                                                                                                             

(*) `GP2GP_GPC_GET_URL` could be set to the base URL of a GP Connect Producer for limited testing purposes

### MHS Adaptor Configuration Options

The GP2GP uses the [MHS Adaptor](https://github.com/nhsconnect/integration-adaptor-mhs) to send/receive messages to/from Spine.

| Environment Variable                         | Default                                 | Description                                                                                                                                                                                                              |
|----------------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_MHS_OUTBOUND_URL                       | http://localhost:8081/mock-mhs-endpoint | URL to the MHS adaptor's outbound endpoint                                                                                                                                                                               |
| GP2GP_MHS_INBOUND_QUEUE                      | inbound                                 | Name of the queue for MHS inbound                                                                                                                                                                                        |
| GP2GP_MHS_INBOUND_QUEUE_CONSUMER_CONCURRENCY | 1                                       | Defines the number of concurrent mhs inbound queue consumers in a single application. https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/annotation/JmsListener.html#concurrency-- |


### GP2GP Configuration Options

| Environment Variable             | Default | Description                                                                                                                                                      |
|----------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_LARGE_ATTACHMENT_THRESHOLD | 4500000 | Value in bytes. Defines the max size of a single attachment sent to MHS. If a document is larger than this value, it's content will be split and sent in chunks. | 

### MHS Adaptor and GP Connect Consumer Adaptor Client Options
Options for configuring the web client making requests to the MHS Adaptor and the GP Connect Consumer Adaptor.

Backoff options are used to perform retries of a HTTP request when the client experiences a timeout or 5xx response.
The client will perform retries up to the number provided by `MAX_BACKOFF_ATTEMPTS`.
Time to wait before making the next request attempt is calculated using the equation below, with `retryNumber` starting
at 0 for the first failure, and `jitter` being a random number in the range 0.5 - 1.5

```math
\text{timeToWait} = \text{minBackoffSeconds} \times 2.0^\text{retryNumber} \times jitter
```

Timeout is measured as total time for the HTTP request and response to complete.

#### GP Connect Consumer Adaptor client

| Environment Variable                  | Default | Description                 |
|---------------------------------------|---------|-----------------------------|
| GP2GP_GPC_CLIENT_MAX_BACKOFF_ATTEMPTS | 6       | Max backoff attempts        |
| GP2GP_GPC_CLIENT_MIN_BACKOFF_SECONDS  | 5       | Min Backoff time (seconds)  |
| GP2GP_GPC_CLIENT_TIMEOUT_SECONDS      | 1200    | Request timeout (seconds)   |

#### MHS Adaptor client

| Environment Variable                  | Default | Description                 |
|---------------------------------------|---------|-----------------------------|
| GP2GP_MHS_CLIENT_MAX_BACKOFF_ATTEMPTS | 6       | Max backoff attempts        |
| GP2GP_MHS_CLIENT_MIN_BACKOFF_SECONDS  | 5       | Min Backoff time (seconds)  |
| GP2GP_MHS_CLIENT_TIMEOUT_SECONDS      | 120     | Request timeout (seconds)   |

## Adaptor Process

TODO: Sequence diagram "user journey" of Spine messages, tasks, and GPCC requests

Adaptor document tasks are defined and documented in Java source code:

* [TaskDefinition.java](https://github.com/nhsconnect/integration-adaptor-gp2gp/tree/main/service/src/main/java/uk/nhs/adaptors/gp2gp/common/task/TaskDefinition.java)
* [DocumentTaskDefinition.java](https://github.com/nhsconnect/integration-adaptor-gp2gp/tree/main/service/src/main/java/uk/nhs/adaptors/gp2gp/ehr/DocumentTaskDefinition.java) 
  * [Get GPC Document Task Example](/documentation/examples/Task_queue_payloads/GetGpcDocumentTaskDefinition.md)
* [SendEhrExtractCoreTaskDefinition.java](https://github.com/nhsconnect/integration-adaptor-gp2gp/tree/main/service/src/main/java/uk/nhs/adaptors/gp2gp/ehr/SendEhrExtractCoreTaskDefinition.java) 
  * [Send EHR Extract Core Task Example](/documentation/examples/Task_queue_payloads/SendEhrExtractCoreTaskDefinition.md)
* [GetGpcStructuredTaskDefinition.java](https://github.com/nhsconnect/integration-adaptor-gp2gp/tree/main/service/src/main/java/uk/nhs/adaptors/gp2gp/gpc/GetGpcStructuredTaskDefinition.java) 
  * [Get GPC Structured Record Task Example](/documentation/examples/Task_queue_payloads/GetGpcStructuredTaskDefinition.md)
* [SendAcknowledgementTaskDefinition.java](https://github.com/nhsconnect/integration-adaptor-gp2gp/tree/main/service/src/main/java/uk/nhs/adaptors/gp2gp/ehr/SendAcknowledgementTaskDefinition.java) 
  * [Send Acknowledgement Task Example](/documentation/examples/Task_queue_payloads/sendAcknowledgementTaskDefinition.md)

## Logging and Tracing

```
yyyy-MM-dd HH:mm:ss.SSS Level=INFO Logger=u.n.a.g.e.m.AgentDirectoryExtractor ConversationId=18262a7b-cb4c-4aef-8dd8-7ffc3dda67aa TaskId=ad4e53cc-9b4e-4e07-8937-f0f91887b21d Thread=java-thread-1 Message="The log message"
```

* Level: The logging level DEBUG/INFO/WARN/ERROR
* Logger: The name of the Java class that emitted the message
* ConversationId: The identifier correlating all GP2GP message for a single patient transfer
* TaskId: The unique identifier of the task
* Message: The log message

## Object Storage (AWS S3 / Azure Blob)

- Data stored:
  - The patients transformed structured record once it has been transformed into an ehrExtract that is ready to send to MHS Outbound
  - Attachments associated with the patients structured record once they have been transformed into an outbound message for sending to MHS Outbound
- Filename convention:
  - EhrExtracts files are named as a concatenation of {conversationId}/{conversationId}.json
  - Attachment files are named as {conversationId}/{documentId}.json where documentId is the name of the file
- Metadata stored with files
  - Type - Task type that uploaded the file GET_GPC_STRUCTURED / GET_GPC_DOCUMENT
  - ConversationId - Task conversation ID
  - TaskId - Task ID

## AMQP Message Broker Requirements

* The broker must be configured with a limited number of retries and dead-letter queues
* It is the responsibility of the GP supplier to configure adequate monitoring against the dead-letter queues that allows ALL undeliverable messages to be investigated fully.
* The broker must use persistent queues to avoid loss of data

**Using AmazonMQ**

* A persistent broker (not in-memory) must be used to avoid data loss.
* A configuration profile that includes settings for [retry and dead-lettering](https://activemq.apache.org/message-redelivery-and-dlq-handling.html) must be applied.
* AmazonMQ uses the scheme `amqp+ssl://` but this **MUST** be changed to `amqps://` when configuring the adaptor.

**Using Azure Service Bus**

* The ASB must use [MaxDeliveryCount and dead-lettering](https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues#exceeding-maxdeliverycount)
* Azure Service Bus may require some parameters as part of the URL configuration. For example: `GP2GP_AMQP_BROKERS=amqps://<NAME>.servicebus.windows.net/;SharedAccessKeyName=<KEY NAME>;SharedAccessKey=<KEY VALUE>`

## Message Queues and Content

The adaptor subscribes to the [MHS Adaptor](https://github.com/nhsconnect/integration-adaptor-mhs)'s 
inbound queue to receive messages from Spine. Refer to the MHS Adaptor documentation to 
learn about these messages. 

TODO: Document task queue payloads

## Database Requirements

* The GP2GP adaptor must be used with Mongodb-compatible database such as Amazon Document DB or Azure Cosmos
* The adaptor stores the identifiers, status, and metadata for each patient transfer
* Deleting the mongodb database, its collections, or its records will cause any in-progress transfers to fail
* The database should be used to monitor for any failed or incomplete transfers

**Amazon Document DB Tips**

In the "Connectivity & security" tab of the cluster a URI is provided to "Connect to this cluster with an application".
Replace <username>:<insertYourPasswordHere> with the actual mongo username and password to be used by the application.
The value of `GP2GP_MONGO_URI` should be set to this value. Since the URI string contains credentials we recommend 
managing the entire value as a secured secret.

The user must have the `readWrite` role or a custom role with specific privileges.

**Azure Cosmos DB Tips**

Follow Azure documentation on Cosmos DB's API for MongoDB

## Database Design

Refer to [database.md](/documentation/database/database.md) for design details

## Detecting Failed GP2GP Transfers

The supplier must monitor the adaptor to detect failed GP2GP EHR Transfers.

`db.inventory.find({error: {$exists: true}}, {error: 1})`

Query will select the extracts where an error has occurred from the MongoDb

Detecting incomplete transfers, finds entries between the dates provided and where the process either has not reached the point of sending the acknowledgement or if it has been sent it has been sent in error: 

`db.inventory.find({created: {$gte:ISODate('2021-10-08'), $lt:ISODate('2021-10-09')}, $or:[{ackToRequester: {$exists: false}}, {"ackPending.typeCode": "AE"}]})`

## Retention of patient data

The adaptor does NOT fulfill the supplier's obligation under NPFIT-FNT-TO-IG-DES-0115.01 for
long-term data retention. The adaptor does also NOT fulfill HSCIC-PC-BLD-0068.26 requirements 
related to data retention including BR15 and S63.

The adaptor downloads and translates the patient's record in its entirety (including attachments)
before transmitting any portion of the record. The adaptor stages the translated portions in 
Object Storage (AWS S3 / Azure Blob). The supplier MUST configure a lifecycle policy
in their selected storage solution to remove these records after a reasonable time period.
The adaptor does NOT control the retention of data in object storage.

The adaptor's database records:
* the patient's NHS number
* identifiers (ODS & ASID) of the two GP practices involved in the transfer
* metadata about the transfer process

The supplier MUST configure the `GP2GP_MONGO_TTL` variable to remove the database records
after a reasonable time period. The specs say 12 weeks (84 days), so this is our suggestion.

The adaptor's queued messages contain:
* the patient's NHS number
* identifiers (ODS & ASID) of the two GP practices involved in the transfer
* metadata about the transfer process

The supplier MUST monitor the broker's dead-letter queues to investigate any errors and clear 
old messages after a reasonable time period.

## Known receiving system limits

During end-to-end testing between different GP system implementations of GP2GP we observed the following limits.

*  One implementation had a limit on the size of each individual attachment sent to it of 100MB.
   If an attachment sent to it exceeds this they will not file it into the patient record, but file a placeholder instead.
*  One implementation had a limit on the total size of all attachments sent to it, equalling 375MB (≈ 500MB encoded as base64).
   If a patient record exceeds this they will send a "Large Message general failure" response to the `IN030000UK06` message.

## Exemplar Deployment

We release adaptor image on Dockerhub as [nhsdev/nia-gp2gp-adaptor][docker-hub-image],
with the latest changes documented within the [CHANGELOG.md](CHANGELOG.md).

When performing assurance against a simulated workload involving the transfer of 100MB attachments, we
have identified a minimum of 8GB of RAM and 2 vCPUs to the container is required.

We provide [Terraform scripts][exemplar-deployment] to perform an exemplar deployment of the GP2GP adaptor and its
dependencies into AWS.

[exemplar-deployment]: https://github.com/nhsconnect/integration-adaptors/tree/develop/terraform/aws/components/gp2gp
[docker-hub-image]: https://hub.docker.com/r/nhsdev/nia-gp2gp-adaptor