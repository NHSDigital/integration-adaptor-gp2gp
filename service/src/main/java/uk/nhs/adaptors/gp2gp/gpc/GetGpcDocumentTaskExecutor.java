package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.JSON_EXTENSION;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final GpcClient gpcClient;
    private final GpcDocumentTranslator gpcDocumentTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final RandomIdGeneratorService randomIdGeneratorService;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");

        var response = gpcClient.getDocumentRecord(taskDefinition);

        String documentName = taskDefinition.getDocumentId() + JSON_EXTENSION;
        String taskId = taskDefinition.getTaskId();
        String messageId = randomIdGeneratorService.createNewId();

        String mhsOutboundRequestData = gpcDocumentTranslator.translateToMhsOutboundRequestData(taskDefinition, response, messageId);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentName);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(taskDefinition, documentName,
            taskId, messageId);
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
