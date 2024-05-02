package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.ehr.utils.AbsentAttachmentUtils.buildAbsentAttachmentFileName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.utils.Base64Utils;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AbsentAttachmentFileMapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.DetectTranslationCompleteService;
import uk.nhs.adaptors.gp2gp.gpc.DocumentToMHSTranslator;
import uk.nhs.adaptors.gp2gp.gpc.StorageDataWrapperProvider;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetAbsentAttachmentTaskExecutor implements TaskExecutor<GetAbsentAttachmentTaskDefinition> {

    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DocumentToMHSTranslator documentToMHSTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<GetAbsentAttachmentTaskDefinition> getTaskType() {
        return GetAbsentAttachmentTaskDefinition.class;
    }

    @Override
    public void execute(GetAbsentAttachmentTaskDefinition taskDefinition) {
        var ehrExtractStatus = handleAbsentAttachment(taskDefinition);
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    public EhrExtractStatus handleAbsentAttachment(DocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();

        var fileContent = Base64Utils.toBase64String(AbsentAttachmentFileMapper.mapDataToAbsentAttachment(
            taskDefinition.getOriginalDescription(),
            taskDefinition.getToOdsCode(),
            taskDefinition.getConversationId()
        ));

        final var storagePath = buildAbsentAttachmentFileName(taskDefinition.getDocumentId());
        final var fileName = buildAbsentAttachmentFileName(taskDefinition.getDocumentId());

        var mhsOutboundRequestData = documentToMHSTranslator.translateFileContentToMhsOutboundRequestData(taskDefinition, fileContent);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, storagePath);

        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, storagePath, fileContent.length(), getTitle(taskDefinition), fileName
        );
    }

    private String getTitle(DocumentTaskDefinition taskDefinition) {
        if (taskDefinition.getTitle() != null) {
            return taskDefinition.getTitle();
        }

        return "The document could not be retrieved";
    }
}
