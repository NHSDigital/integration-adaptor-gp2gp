package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.utils.BinaryUtils.getBytesLengthOfString;
import static uk.nhs.adaptors.gp2gp.common.utils.Gzip.compress;
import static uk.nhs.adaptors.gp2gp.ehr.utils.AbsentAttachmentUtils.buildAbsentAttachmentFileName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.GetAbsentAttachmentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    public static final String SKELETON_ATTACHMENT = "X-GP2GP-Skeleton: Yes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String XML_CONTENT_TYPE = "application/xml";
    public static final String TEXT_XML_CONTENT_TYPE = "text/xml";
    private static final String LEADING_UNDERSCORE_CHAR = "_";

    private final TimestampService timestampService;
    private final GpcClient gpcClient;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final MessageContext messageContext;
    private final FhirParseService fhirParseService;
    private final ObjectMapper objectMapper;
    private final StructuredRecordMappingService structuredRecordMappingService;
    private final TaskDispatcher taskDispatcher;
    private final Gp2gpConfiguration gp2gpConfiguration;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EhrDocumentMapper ehrDocumentMapper;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        String ehrExtractXml;
        Instant now = this.timestampService.now();
        List<OutboundMessage.ExternalAttachment> externalAttachments = new ArrayList<>();
        List<OutboundMessage.ExternalAttachment> absentAttachments = new ArrayList<>();
        List<EhrExtractStatus.GpcDocument> ehrStatusGpcDocuments = new ArrayList<>();

        // Part 1.
        Bundle structuredRecord = getStructuredRecord(structuredTaskDefinition);

        // TODO: Add some code which stores this `structuredRecord` variable in S3, so it can be retrieved in part2.

        // Part 2.
        try {
            messageContext.initialize(structuredRecord);

            ehrExtractXml = structuredRecordMappingService
                    .mapStructuredRecordToEhrExtractXml(structuredTaskDefinition, structuredRecord);


            LOGGER.info("Checking EHR Extract size");
            if (isLargeEhrExtract(ehrExtractXml)) {
                LOGGER.info("EHR extract IS large");
                var realEhrExtract = ehrExtractXml;

                ehrExtractXml = Base64Utils.toBase64String(compress(ehrExtractXml));

                var messageId = randomIdGeneratorService.createNewId();
                var documentId = randomIdGeneratorService.createNewId();
                var fileName = GpcFilenameUtils.generateLargeExrExtractFilename(documentId);
                var compressedAndEncodedEhrExtractSize = ehrExtractXml.length();

                var largeEhrExtractXmlAsExternalAttachment = buildExternalAttachmentForLargeEhrExtract(
                        compressedAndEncodedEhrExtractSize, messageId, documentId, fileName
                );

                // MODIFIES ExternalAttachments so that the EhrExtract MHS request contains a reference to the compressed XML.
                // This is fine if we have the ExternalAttachments generation inside of Part 2.
                externalAttachments.add(largeEhrExtractXmlAsExternalAttachment);

                var getDocumentTaskDefinition = buildGetDocumentTask(structuredTaskDefinition, largeEhrExtractXmlAsExternalAttachment);
                var mhsPayload = ehrDocumentMapper.mapMhsPayloadTemplateToXml(
                        ehrDocumentMapper.mapToMhsPayloadTemplateParameters(getDocumentTaskDefinition, XML_CONTENT_TYPE));

                // MODIFIES S3
                uploadToStorage(ehrExtractXml, mhsPayload, fileName, getDocumentTaskDefinition);

                // This following line could (in theory) be replaced with a call to:
                // ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences
                ehrStatusGpcDocuments.add(EhrExtractStatus.GpcDocument.builder()
                        .documentId(documentId)
                        .accessDocumentUrl(null)
                        .contentType(TEXT_XML_CONTENT_TYPE)
                        .objectName(fileName)
                        .fileName(fileName)
                        .accessedAt(now)
                        .taskId(getDocumentTaskDefinition.getTaskId())
                        .messageId(messageId)
                        .isSkeleton(true)
                        .identifier(null)
                        .build());

                // MODIFIES ehrExtractXML which is fine if we are generating the XML in part 2.
                ehrExtractXml = structuredRecordMappingService.buildSkeletonEhrExtractXml(realEhrExtract, documentId);
            }
            // End of part 2.

            // Mixture of Part 1 and Part 2.
            // We need to keep the population of ehrStatusGpcDocuments in Part 1.
            // Move the population of externalAttachments / absentAttachments to part 2.

            // Uses the structuredRecordMappingService, and needs the JSON -> XML conversion to have happened,
            // because we depend on the IDs being the same.


            var documentsAsExternalAttachments = structuredRecordMappingService
                    .getExternalAttachments(structuredRecord);
            documentsAsExternalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isBlank(documentMetadata.getUrl()))
                .peek(absentAttachment ->
                        LOGGER.warn("DocumentReference missing URL: {}", absentAttachment.getDocumentId()))
                .forEach(absentAttachments::add);

            documentsAsExternalAttachments = documentsAsExternalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isNotBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());

            documentsAsExternalAttachments.stream()
                .map(externalAttachment -> EhrExtractStatus.GpcDocument.builder()
                    .documentId(externalAttachment.getDocumentId()) // Dependency here where we need the document ID in part 1, but the value is coming from the mapping service.
                    .accessDocumentUrl(externalAttachment.getUrl())
                    .objectName(null)
                    .accessedAt(now)
                    .taskId(structuredTaskDefinition.getTaskId())
                    .messageId(structuredTaskDefinition.getConversationId())
                    .isSkeleton(false)
                    .identifier(externalAttachment.getIdentifier())
                    .fileName(externalAttachment.getFilename()) // Same dependency as above for documentId.
                    .contentType(externalAttachment.getContentType())
                    .originalDescription(externalAttachment.getOriginalDescription())
                    .build())
                .forEach(ehrStatusGpcDocuments::add);
            externalAttachments.addAll(documentsAsExternalAttachments);

            // Uses the structuredRecordMappingService, and needs the JSON -> XML conversion to have happened,
            // because we depend on the IDs being the same.
            //   The document ID is referenced in the DocumentReference within the EHR extract
            //   The document ID is referenced by the EHR Extract external documents array.
            // If we moved both of these into Part 2, then the IDs will still be consistent.
            absentAttachments.addAll(structuredRecordMappingService.getAbsentAttachments(structuredRecord));

            absentAttachments.stream()
                .map(absentAttachment -> EhrExtractStatus.GpcDocument.builder()
                    .documentId(absentAttachment.getDocumentId())
                    .fileName(buildAbsentAttachmentFileName(absentAttachment.getDocumentId()))
                    .contentType(StructuredRecordMappingService.DEFAULT_ATTACHMENT_CONTENT_TYPE)
                    .accessDocumentUrl(null)
                    .objectName(null)
                    .accessedAt(now)
                    .taskId(structuredTaskDefinition.getTaskId())
                    .messageId(structuredTaskDefinition.getConversationId())
                    .isSkeleton(false)
                    .identifier(absentAttachment.getIdentifier())
                    .originalDescription(absentAttachment.getOriginalDescription())
                    .build())
                .forEach(ehrStatusGpcDocuments::add);


            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
                structuredTaskDefinition, ehrStatusGpcDocuments
            );
            queueGetDocumentsTask(structuredTaskDefinition, documentsAsExternalAttachments);
            queueGetAbsentAttachmentTask(structuredTaskDefinition, absentAttachments);

            // End Part 1.
        } finally {
            messageContext.resetMessageContext();
        }

        // Part 2 would need to either Update the XML here OR do the conversion from JSON to XML.
        // If we wanted to perform JSON to XML conversion in part 2:
        //   - We'd have to copy all the Parameters in structuredTaskDefinition needed for the XML, which includes the ASIDs
        //     - We believe this data is already stored within the EhrExtractStatus (Mongo)
        //   - Store the JSON somewhere.
        //   - Pull out the documents from the Mongo DB, and convert them to External attachments.
        // Want to try going down this route, because we believe it'll generate simpler/cleaner code.
        // What are the highest risks/blockers to this, and how do we prioritise those?
        //  - When we generate the GpcDocument objects to be stored in Mongo, we use the Document ID (PK) which has
        //    come from the XML mapping.  We'd either need to use a different PK, or somehow ensure the StructuredRecordMappingService
        //    was populated with the FHIR ID -> Document ID mappings; in either case start storing the FHIR DocumentReference ID.

        // If we wanted to perform the JSON to XML conversion in part 1:
        //   - We'd need to iterate through each Document in DB and update the XML NarrativeStatement where an attachment is absent.
        //   - We'd have to create a mechanism to update the contents of the structuredRecord in S3 if it were a skeleton
        //     (Which would involve calling uploadFile with the `generateStructuredRecordFilename`)



        // Fetch these attachments from the DocumentDB.
        // Will need some mechanism to convert from EhrExtractStatus.GpcDocument to OutboundMessage.ExternalAttachment
        var allExternalAttachments = Stream
            .concat(externalAttachments.stream(), absentAttachments.stream())
            .collect(Collectors.toList());

        // This stores the EHR Extract response into S3, so that we can send it as part of SendEhrExtractCoreTaskExecutor
        // This can be moved over to Part 2.
        // Inputs:
        //  - The EHR Extract, which may or may not be a skeleton message.
        //  - The list of all external attachments. Which will need to come from the DocumentDB.
        var stringRequestBody = objectMapper.writeValueAsString(OutboundMessage.builder()
            .payload(ehrExtractXml)
            .attachments(Collections.emptyList())
            .externalAttachments(mapPrefixesToDocumentIds(allExternalAttachments))
            .build()
        );

        var storageDataWrapper = StorageDataWrapperProvider.buildStorageDataWrapper(
            structuredTaskDefinition,
            stringRequestBody,
            structuredTaskDefinition.getTaskId()
        );

        var structuredRecordJsonFilename = GpcFilenameUtils.generateStructuredRecordFilename(
            structuredTaskDefinition.getConversationId()
        );

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
        // End of part 2.
    }

    private Bundle getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
    }

    private void queueGetDocumentsTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .filter(externalAttachment -> StringUtils.isNotBlank(externalAttachment.getUrl()))
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private void queueGetAbsentAttachmentTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> absentAttachments) {
        absentAttachments.stream()
            .map(absentAttachment -> buildGetAbsentAttachmentTask(taskDefinition, absentAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private GetAbsentAttachmentTaskDefinition buildGetAbsentAttachmentTask(
        TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment absentAttachment) {
        return GetAbsentAttachmentTaskDefinition.builder()
            .documentId(absentAttachment.getDocumentId())
            .title(absentAttachment.getTitle())
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .messageId(absentAttachment.getMessageId())
            .originalDescription(absentAttachment.getOriginalDescription())
            .build();
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(
        TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment externalAttachment) {

        return GetGpcDocumentTaskDefinition.builder()
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .documentId(externalAttachment.getDocumentId())
            .accessDocumentUrl(externalAttachment.getUrl())
            .messageId(externalAttachment.getMessageId())
            .title(externalAttachment.getTitle())
            .originalDescription(externalAttachment.getOriginalDescription())
            .build();
    }

    private boolean isLargeEhrExtract(String ehrExtract) {
        return getBytesLengthOfString(ehrExtract) > gp2gpConfiguration.getLargeEhrExtractThreshold();
    }

    private List<OutboundMessage.ExternalAttachment> mapPrefixesToDocumentIds(List<OutboundMessage.ExternalAttachment> extAttachments) {
        return extAttachments.stream()
            .peek(externalAttachment -> externalAttachment.setDocumentId(
                LEADING_UNDERSCORE_CHAR.concat(externalAttachment.getDocumentId())
            ))
            .collect(Collectors.toList());
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachmentForLargeEhrExtract(
        int compressedAndEncodedEhrExtractSize, String messageId, String documentId, String fileName) {

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .filename(fileName)
            .description(OutboundMessage.AttachmentDescription.builder()
                .fileName(fileName)
                .contentType(TEXT_XML_CONTENT_TYPE)
                .length(compressedAndEncodedEhrExtractSize)
                .compressed(true)
                .largeAttachment(compressedAndEncodedEhrExtractSize > gp2gpConfiguration.getLargeAttachmentThreshold())
                .originalBase64(false)
                .domainData(SKELETON_ATTACHMENT)
                .build()
                .toString())
            .url(StringUtils.EMPTY)
            .build();
    }

    @SneakyThrows
    private void uploadToStorage(String ehrExtract, String mhsPayload, String fileName, DocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();

        var attachments = Collections.singletonList(
            OutboundMessage.Attachment.builder()
                .contentType(TEXT_XML_CONTENT_TYPE)
                .isBase64(Boolean.TRUE)
                .description(taskDefinition.getDocumentId())
                .payload(ehrExtract)
                .build());
        var outboundMessage = OutboundMessage.builder()
            .payload(mhsPayload)
            .attachments(attachments)
            .build();

        var outboundMessageString = OBJECT_MAPPER.writeValueAsString(outboundMessage);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, outboundMessageString, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, fileName);
    }
}
