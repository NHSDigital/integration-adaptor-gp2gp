package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.utils.StringChunking.chunkEhrExtract;
import static uk.nhs.adaptors.gp2gp.common.utils.StringChunking.getBytesLengthOfString;

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
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

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
                List<OutboundMessage.ExternalAttachment> chunkedEhrExtractAttachments = new ArrayList<>();
                var chunkedHL7 = chunkEhrExtract(compressedHL7, gp2gpConfiguration.getLargeEhrExtractThreshold() - 2700);
                for (int i = 0; i < chunkedHL7.size(); i++) {
                    var chunk = chunkedHL7.get(i);
                    var externalAttachment = buildExternalAttachment(chunk, structuredTaskDefinition);
                    chunkedEhrExtractAttachments.add(externalAttachment);

                    var taskDefinition = buildGetDocumentTask(structuredTaskDefinition, externalAttachment);
                    uploadToStorage(chunk, externalAttachment.getFilename(), taskDefinition);
                    ehrExtractStatusService.updateEhrExtractStatusWithEhrExtractChunks(structuredTaskDefinition, externalAttachment);
                }

                // generate binding doc and reference from skeleton message to be sent in place of HL7
                var bindingDoc = buildBindingDocument(chunkedEhrExtractAttachments);
                // generate special EhrComposition with Narrative statement referencing binding doc

                hl7TranslatedResponse = structuredRecordMappingService.getHL7ForLargeEhrExtract(structuredTaskDefinition, bindingDoc);

                externalAttachments.addAll(chunkedEhrExtractAttachments);

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

    private OutboundMessage.ExternalAttachment buildExternalAttachment(String chunk, TaskDefinition taskDefinition) {
        var messageId = randomIdGeneratorService.createNewId();
        var documentId = randomIdGeneratorService.createNewId();
        var documentName = GpcFilenameUtils.generateDocumentFilename(
            taskDefinition.getConversationId(), documentId
        );

        return OutboundMessage.ExternalAttachment.builder()
            .documentId(documentId)
            .messageId(messageId)
            .contentType("application/xml") // confirm this is correct
            .filename(documentName)
            .length(getBytesLengthOfString(chunk))
            .compressed(true)
            .largeAttachment(false)
            .originalBase64(true) // always true since GPC gives us a Binary resource which is mandated to have base64 encoded data
            .url(StringUtils.EMPTY)
            .build();
    }

    private void uploadToStorage(String chunk, String documentName, DocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var mhsOutboundRequestData = gpcDocumentTranslator.translateToMhsOutboundRequestData(taskDefinition, chunk);
        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);
    }

    // placeholder
    private String buildBindingDocument(List<OutboundMessage.ExternalAttachment> attachments) {
        return randomIdGeneratorService.createNewId();
    }
}
