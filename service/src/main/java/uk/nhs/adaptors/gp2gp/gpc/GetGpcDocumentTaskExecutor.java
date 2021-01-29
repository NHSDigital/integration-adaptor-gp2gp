package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private static final String JSON_EXTENSION = ".json";
    private static final String MHS_FILE_NAME_TEMPLATE = "%s_doc_translated.json";

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

        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(taskDefinition);
        var response = gpcClient.getDocumentRecord(request, taskDefinition);

        String documentName = taskDefinition.getDocumentId() + JSON_EXTENSION;
        String taskId = taskDefinition.getTaskId();

        var storageDataWrapperWithDocumentRecord = StorageDataWrapperProvider.buildStorageDataWrapper(taskDefinition, response, taskId);
        storageConnectorService.uploadFile(storageDataWrapperWithDocumentRecord, documentName);

        String messageId = randomIdGeneratorService.createNewId();
        String mhsOutboundRequest = gpcDocumentTranslator.translateToMhsOutboundRequestPayload(taskDefinition, response, messageId);
        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider.buildStorageDataWrapper(taskDefinition,
            mhsOutboundRequest, taskId);
        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, prepareXmlDocumentName(messageId));

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(taskDefinition, documentName,
            taskId, messageId);
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
    private String prepareXmlDocumentName(String messageId) {
        return String.format(MHS_FILE_NAME_TEMPLATE, messageId);
    }
}
