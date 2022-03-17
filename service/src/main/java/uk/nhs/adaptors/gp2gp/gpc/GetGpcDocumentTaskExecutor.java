package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.GetAbsentAttachmentTaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final GpcClient gpcClient;
    private final DocumentToMHSTranslator documentToMHSTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final FhirParseService fhirParseService;
    private final GetAbsentAttachmentTaskExecutor getAbsentAttachmentTaskExecutor;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        EhrExtractStatus ehrExtractStatus;
        try {
            var binaryContent = getBinaryContent(taskDefinition);
            ehrExtractStatus = handleValidGpcDocument(binaryContent, taskDefinition);
        } catch (GpConnectException e) {
            LOGGER.warn("Binary request returned an unexpected response", e);
            ehrExtractStatus = getAbsentAttachmentTaskExecutor.handleAbsentAttachment(taskDefinition);
        }

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private EhrExtractStatus handleValidGpcDocument(BinaryContent binaryContent, GetGpcDocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var documentName = GpcFilenameUtils.generateDocumentFilename(
            taskDefinition.getConversationId(), taskDefinition.getDocumentId()
        );

        var storageDataWrapperWithMhsOutboundRequest = getStorageDataWrapper(binaryContent, taskDefinition, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);

        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, documentName, taskId, taskDefinition.getMessageId(), binaryContent.getContentAsBase64().length());
    }

    private StorageDataWrapper getStorageDataWrapper(BinaryContent binaryContent, GetGpcDocumentTaskDefinition taskDefinition, String taskId) {
        var mhsOutboundRequestData = documentToMHSTranslator.translateGpcResponseToMhsOutboundRequestData(
            taskDefinition,
            binaryContent.getContentAsBase64(),
            binaryContent.getContentType()
        );

        return StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);
    }

    public BinaryContent getBinaryContent(GetGpcDocumentTaskDefinition taskDefinition) {
        var response = gpcClient.getDocumentRecord(taskDefinition);
        var binary = fhirParseService.parseResource(response, Binary.class);
        return BinaryContent.builder()
            .contentType(new String(binary.getContentType())) // need a new string object to allow GC to dispose the Binary
            .contentLength(binary.getContentAsBase64().length())
            .contentAsBase64(binary.getContentAsBase64()) // this creates a new string object
            .build();
    }

    @Builder
    @Getter
    public static class BinaryContent {
        private final String contentAsBase64;
        private final String contentType;
        private final int contentLength;
    }
}
