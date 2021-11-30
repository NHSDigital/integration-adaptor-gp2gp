package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final GpcClient gpcClient;
    private final DocumentToMHSTranslator gpcDocumentTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        var response = gpcClient.getDocumentRecord(taskDefinition);

        var taskId = taskDefinition.getTaskId();
        var messageId = taskDefinition.getMessageId();
        var documentName = GpcFilenameUtils.generateDocumentFilename(
            taskDefinition.getConversationId(), taskDefinition.getDocumentId()
        );

        var mhsOutboundRequestData = gpcDocumentTranslator.translateGpcResponseToMhsOutboundRequestData(taskDefinition, response);
        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, documentName, taskId, messageId);
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
