package uk.nhs.adaptors.gp2gp.gpc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.model.MhsPayloadTemplateParameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private static final String JSON_EXTENSION = ".json";
    private static final String MHS_FILE_NAME_TEMPLATE = "%s_doc_translated.json";

    @Autowired
    private StorageConnectorService storageConnectorService;
    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;
    @Autowired
    private GpcRequestBuilder gpcRequestBuilder;
    @Autowired
    private GpcClient gpcClient;
    @Autowired
    private DetectTranslationCompleteService detectTranslationCompleteService;
    @Autowired
    private MhsPayloadMapper mhsPayloadMapper;
    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;

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

        String messageId = randomIdGeneratorService.createNewId();

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(taskDefinition, documentName,
            taskId, messageId);

        String xmlPayload = translateToXmlPayload(taskDefinition, messageId);
        var storageDataWrapperWithMhsPayload = StorageDataWrapperProvider.buildStorageDataWrapper(taskDefinition, xmlPayload, taskId);
        storageConnectorService.uploadFile(storageDataWrapperWithMhsPayload, prepareXmlDocumentName(messageId));

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
        var storageDataWrapperWithDocumentRecord = StorageDataWrapperProvider.buildStorageDataWrapper(taskDefinition, response, taskId);
        storageConnectorService.uploadFile(storageDataWrapperWithDocumentRecord, documentName);
    }

    private String translateToXmlPayload(GetGpcDocumentTaskDefinition taskDefinition, String messageId) {
        MhsPayloadTemplateParameters mhsPayloadTemplateParameters =
            mhsPayloadMapper.mapToMhsPayloadTemplateParameters(taskDefinition, messageId);
        return mhsPayloadMapper.mapMhsPayloadTemplateToXml(mhsPayloadTemplateParameters);
    }

    private String prepareXmlDocumentName(String messageId) {
        return String.format(MHS_FILE_NAME_TEMPLATE, messageId);
    }
}
