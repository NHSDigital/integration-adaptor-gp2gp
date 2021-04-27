# Operating

How to operate the GP2GP adaptor

## Adaptor Process

TODO: Sequence diagram "user journey" of Spine messages, tasks, and GPCC requests

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

TODO: Document storage scheme

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
* Azure Service Bus may require some parameters as part of the URL configuration. For example: `NHAIS_AMQP_BROKERS=amqps://<NAME>.servicebus.windows.net/;SharedAccessKeyName=<KEY NAME>;SharedAccessKey=<KEY VALUE>`

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

TODO: Document database design

## Detecting Failed GP2GP Transfers

The supplier must monitor the adaptor to detect failed GP2GP EHR Transfers.

TODO: Database query to detect failed transfers
TODO: Database query to detect incomplete transfers not updated after an amount of time

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
after a reasonable time period.

The adaptor's queued messages contain:
* the patient's NHS number
* identifiers (ODS & ASID) of the two GP practices involved in the transfer
* metadata about the transfer process

The supplier MUST monitor the broker's dead-letter queues to investigate any errors and clear 
old messages after a reasonable time period.

## Exemplar Deployment

https://github.com/nhsconnect/integration-adaptors/tree/develop/terraform/aws/components/gp2gp

We provide Terraform scripts to perform an exemplar deployment of the GP2GP adaptor and its
dependencies into AWS.