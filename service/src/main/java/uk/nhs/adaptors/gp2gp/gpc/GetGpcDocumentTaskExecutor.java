package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.dstu3.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
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
            var response = gpcClient.getDocumentRecord(taskDefinition);
            ehrExtractStatus = handleValidGpcDocument(response, taskDefinition);
        } catch (GpConnectException e) {
            LOGGER.warn("Binary request returned an unexpected response", e);
            ehrExtractStatus = getAbsentAttachmentTaskExecutor.handleAbsentAttachment(taskDefinition);
        }

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private EhrExtractStatus handleValidGpcDocument(String response, GetGpcDocumentTaskDefinition taskDefinition) {
        var binary = fhirParseService.parseResource(response, Binary.class);
        var taskId = taskDefinition.getTaskId();
        var messageId = taskDefinition.getMessageId();
        var documentName = GpcFilenameUtils.generateDocumentFilename(
            taskDefinition.getConversationId(), taskDefinition.getDocumentId()
        );

        var mhsOutboundRequestData = documentToMHSTranslator.translateGpcResponseToMhsOutboundRequestData(
            taskDefinition,
            binary.getContentAsBase64(),
            binary.getContentType()
        );

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);

        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, documentName, taskId, messageId, binary.getContentAsBase64().length());
    }
}
