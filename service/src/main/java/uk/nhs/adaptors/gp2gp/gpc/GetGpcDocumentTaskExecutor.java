package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.JSON_EXTENSION;
import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.PATH_SEPARATOR;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
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
    public void execute(GetGpcDocumentTaskDefinition documentTaskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");

        var response = gpcClient.getDocumentRecord(documentTaskDefinition);

        String messageId = randomIdGeneratorService.createNewId();

        String mhsOutboundRequestData = gpcDocumentTranslator.translateToMhsOutboundRequestData(
            documentTaskDefinition, response, messageId
        );

        String taskId = documentTaskDefinition.getTaskId();

        StorageDataWrapper storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider.buildStorageDataWrapper(
            documentTaskDefinition, mhsOutboundRequestData, taskId
        );

        String documentJsonFilename = documentTaskDefinition.getConversationId()
            .concat(PATH_SEPARATOR)
            .concat(documentTaskDefinition.getDocumentId())
            .concat(JSON_EXTENSION);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, documentJsonFilename);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            documentTaskDefinition, documentJsonFilename, taskId, messageId
        );
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
