package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;


import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {
    private final GpcClient gpcClient;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        var response = gpcClient.getStructuredRecord(structuredTaskDefinition);

        String fileName = structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION;
        storageConnectorService.uploadFile(StorageDataWrapperProvider.buildStorageDataWrapper(structuredTaskDefinition,
            "hl7TranslatedResponse",
            structuredTaskDefinition.getTaskId()),
            fileName);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(structuredTaskDefinition);

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
