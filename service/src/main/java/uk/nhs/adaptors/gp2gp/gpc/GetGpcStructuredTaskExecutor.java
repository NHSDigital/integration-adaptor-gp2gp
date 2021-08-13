package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.utils.DocumentReferenceUtils;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    private static final int THRESHOLD_MINIMUM = 4;

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
    private final GpcDocumentTranslator gpcDocumentTranslator;
    private final RandomIdGeneratorService randomIdGeneratorService;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        String hl7TranslatedResponse;
        List<OutboundMessage.ExternalAttachment> externalAttachments;

        var structuredRecord = getStructuredRecord(structuredTaskDefinition);

        try {
            messageContext.initialize(structuredRecord);

            externalAttachments = structuredRecordMappingService.getExternalAttachments(structuredRecord);

            var documentReferencesWithoutUrl = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());
            if (!documentReferencesWithoutUrl.isEmpty()) {
                LOGGER.warn("Following DocumentReference resources does not have any Attachments with URL: {}",
                    documentReferencesWithoutUrl);
            }

            externalAttachments = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isNotBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());

            var urls = externalAttachments.stream()
                .collect(Collectors.toMap(OutboundMessage.ExternalAttachment::getDocumentId, OutboundMessage.ExternalAttachment::getUrl));
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(structuredTaskDefinition, urls);

            hl7TranslatedResponse = structuredRecordMappingService.getHL7(structuredTaskDefinition, structuredRecord);

            queueGetDocumentsTask(structuredTaskDefinition, externalAttachments);
        } finally {
            messageContext.resetMessageContext();
        }

        // check size
        if (isLargeEhrExtract(hl7TranslatedResponse)) {
            // TODO: 11/08/2021 compression  story NIAD-1059
            String compressedHL7 = hl7TranslatedResponse;
            if (!isLargeEhrExtract(compressedHL7)) {
                hl7TranslatedResponse = compressedHL7;
            } else {
                // generate chunks and binding doc
                var chunkedHL7 = chunkEhrExtract(compressedHL7, gp2gpConfiguration.getLargeEhrExtractThreshold());
                for (int i = 0; i < chunkedHL7.size(); i++) {
                    var chunk = chunkedHL7.get(i);
                    var externalAttachment = buildExternalAttachment(chunk);
                    externalAttachments.add(externalAttachment);
                    uploadToStorage(chunk, buildGetDocumentTask(structuredTaskDefinition, externalAttachment));
                }

                // generate binding doc and reference from skeleton message to be sent in place of HL7

                // generate special EhrComposition with Narrative statement referencing binding doc
                hl7TranslatedResponse = generateSkeletonPayload(hl7TranslatedResponse);

            }
        }

        var outboundMessage = OutboundMessage.builder()
            .payload(hl7TranslatedResponse)
            .externalAttachments(externalAttachments)
            .build();

        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);
        StorageDataWrapper storageDataWrapper = StorageDataWrapperProvider.buildStorageDataWrapper(
            structuredTaskDefinition,
            stringRequestBody,
            structuredTaskDefinition.getTaskId()
        );

        String structuredRecordJsonFilename = GpcFilenameUtils.generateStructuredRecordFilename(
            structuredTaskDefinition.getConversationId()
        );

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private Bundle getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
    }

    private void queueGetDocumentsTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment externalAttachment) {
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(externalAttachment.getDocumentId())
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(externalAttachment.getUrl())
            .messageId(externalAttachment.getMessageId())
            .build();
    }

    private boolean isLargeEhrExtract(String ehrExtract) {
        return getBytesLengthOfString(ehrExtract) > gp2gpConfiguration.getLargeEhrExtractThreshold();
    }

    private int getBytesLengthOfString(String input) {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }

    private OutboundMessage.ExternalAttachment buildExternalAttachment(String chunk) {
        var documentId = randomIdGeneratorService.createNewId();
        var messageId = randomIdGeneratorService.createNewId();

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .contentType("application/xml") // confirm this is correct
            .filename("")//generate filename
            .length(getBytesLengthOfString(chunk))
            .compressed(true)
            .largeAttachment(false)
            .originalBase64(true) // always true since GPC gives us a Binary resource which is mandated to have base64 encoded data
            .url(StringUtils.EMPTY) // need to workout a way around attachment retrieval
            .build();
    }

    private List<String> chunkEhrExtract(String ehrExtract, int sizeThreshold) {
        if (sizeThreshold <= THRESHOLD_MINIMUM) {
            throw new IllegalArgumentException("SizeThreshold must be larger 4 to hold at least 1 UTF-16 character");
        }

        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < ehrExtract.length(); i++) {
            var c = ehrExtract.charAt(i);
            var chunkBytesSize = chunk.toString().getBytes(StandardCharsets.UTF_8).length;
            var charBytesSize = Character.toString(c).getBytes(StandardCharsets.UTF_8).length;
            if (chunkBytesSize + charBytesSize > sizeThreshold) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(c);
        }
        if (chunk.length() != 0) {
            chunks.add(chunk.toString());
        }

        return chunks;
    }

    private void uploadToStorage(String chunk, DocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var documentName = GpcFilenameUtils.generateDocumentFilename(
            taskDefinition.getConversationId(), taskDefinition.getDocumentId()
        );

        var mhsOutboundRequestData = gpcDocumentTranslator.translateToMhsOutboundRequestData(taskDefinition, chunk);
        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);
    }

    // place holder
    private String generateSkeletonPayload(String id) {
        return id;
    }
}
