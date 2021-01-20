package uk.nhs.adaptors.gp2gp.gpc;

import java.util.UUID;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private static final String JSON_EXTENSION = ".json";

    @Autowired
    private StorageConnectorService storageConnectorService;
    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;
    @Autowired
    private GpcRequestBuilder gpcRequestBuilder;
    @Autowired
    private GpcClient gpcClient;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition documentTaskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");

        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(documentTaskDefinition);
        var response = gpcClient.getDocumentRecord(request, documentTaskDefinition);

        String documentName = documentTaskDefinition.getDocumentId() + JSON_EXTENSION;
        String taskId = UUID.randomUUID().toString();
        storageConnectorService.uploadDocument(documentName, buildStorageDataWrapper(documentTaskDefinition, response, taskId));
        ehrExtractStatusService.updateEhrExtractStatusAccessDocument(documentTaskDefinition, documentName, taskId);
    }

    private StorageDataWrapper buildStorageDataWrapper(GetGpcDocumentTaskDefinition documentTaskDefinition, String response, String taskId) {
        return StorageDataWrapper.builder()
            .type(documentTaskDefinition.getTaskType().getTaskTypeHeaderValue())
            .conversationId(documentTaskDefinition.getConversationId())
            .taskId(taskId)
            .response(response)
            .build();
    }
}
