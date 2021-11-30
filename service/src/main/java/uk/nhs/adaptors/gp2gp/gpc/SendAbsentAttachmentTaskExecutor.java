package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.SendAbsentAttachmentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AbsentAttachmentFileMapper;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class SendAbsentAttachmentTaskExecutor implements TaskExecutor<SendAbsentAttachmentTaskDefinition> {

    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DocumentToMHSTranslator gpcDocumentTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<SendAbsentAttachmentTaskDefinition> getTaskType() {
        return SendAbsentAttachmentTaskDefinition.class;
    }

    @Override
    public void execute(SendAbsentAttachmentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var messageId = taskDefinition.getMessageId();

        var fileContent = AbsentAttachmentFileMapper.mapDataToAbsentAttachment(
            taskDefinition.getTitle(),
            taskDefinition.getToOdsCode(),
            taskDefinition.getConversationId()
        );

        var mhsOutboundRequestData = gpcDocumentTranslator.translateFileContentToMhsOutboundRequestData(taskDefinition, fileContent);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, fileContent);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, taskDefinition.getTitle(), taskId, messageId
        );
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
