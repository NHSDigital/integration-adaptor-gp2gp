package uk.nhs.adaptors.gp2gp.gpc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;

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
    @Autowired
    private GpcTaskAggregateService gpcTaskAggregateService;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");

        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(taskDefinition);
        var response = gpcClient.getDocumentRecord(request, taskDefinition);

        String documentName = taskDefinition.getDocumentId() + JSON_EXTENSION;
        String taskId = taskDefinition.getTaskId();

        var storageDataWrapper = StorageDataWrapperProvider.buildStorageDataWrapper(taskDefinition, response, taskId);
        storageConnectorService.uploadFile(storageDataWrapper, documentName);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(taskDefinition, documentName,
            taskId);

        gpcTaskAggregateService.sendData(ehrExtractStatus);
    }
}
