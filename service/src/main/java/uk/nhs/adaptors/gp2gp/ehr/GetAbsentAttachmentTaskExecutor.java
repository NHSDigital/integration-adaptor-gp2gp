package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.ehr.utils.AbsentAttachmentUtils.buildAbsentAttachmentFileName;

import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AbsentAttachmentFileMapper;
import uk.nhs.adaptors.gp2gp.gpc.DetectTranslationCompleteService;
import uk.nhs.adaptors.gp2gp.gpc.StorageDataWrapperProvider;
import uk.nhs.adaptors.gp2gp.gpc.DocumentToMHSTranslator;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public final class GetAbsentAttachmentTaskExecutor implements TaskExecutor<GetAbsentAttachmentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DocumentToMHSTranslator documentToMHSTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final AbsentAttachmentFileMapper absentAttachmentFileMapper;

    @Override
    public Class<GetAbsentAttachmentTaskDefinition> getTaskType() {
        return GetAbsentAttachmentTaskDefinition.class;
    }

    @Override
    public void execute(GetAbsentAttachmentTaskDefinition taskDefinition) {
        var ehrExtractStatus = handleAbsentAttachment(taskDefinition, Optional.empty());
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    public EhrExtractStatus handleAbsentAttachment(DocumentTaskDefinition taskDefinition,
                                                   Optional<String> gpcResponseError) {
        final String taskId = taskDefinition.getTaskId();
        final String fileContent = getBase64FileContent(taskDefinition);

        final var storagePath = buildAbsentAttachmentFileName(taskDefinition.getDocumentId());
        final var fileName = buildAbsentAttachmentFileName(taskDefinition.getDocumentId());

        var mhsOutboundRequestData = documentToMHSTranslator.translateFileContentToMhsOutboundRequestData(taskDefinition, fileContent);

        var storageDataWrapperWithMhsOutboundRequest = getStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, storagePath);

        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition,
            storagePath,
            fileContent.length(),
            getErrorMessage(taskDefinition, gpcResponseError),
            fileName
        );
    }

    private StorageDataWrapper getStorageDataWrapper(DocumentTaskDefinition taskDefinition, String mhsOutboundRequestData, String taskId) {
        return StorageDataWrapperProvider.buildStorageDataWrapper(
            taskDefinition,
            mhsOutboundRequestData,
            taskId
        );
    }

    private String getBase64FileContent(DocumentTaskDefinition taskDefinition) {
        final String fileContent = absentAttachmentFileMapper.mapFileDataToAbsentAttachment(
            taskDefinition.getOriginalDescription(),
            taskDefinition.getToOdsCode(),
            taskDefinition.getConversationId()
        );

        return Base64Utils.toBase64String(fileContent);
    }

    private String getErrorMessage(DocumentTaskDefinition taskDefinition, Optional<String> exceptionDisplay) {
        final String fallbackMessage = "The document could not be retrieved";
        final String message = exceptionDisplay.orElseGet(taskDefinition::getTitle);

        return StringUtils.isNotBlank(message) ? message : fallbackMessage;
    }
}