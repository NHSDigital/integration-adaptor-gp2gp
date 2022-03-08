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
import uk.nhs.adaptors.gp2gp.gpc.DetectTranslationCompleteService;
import uk.nhs.adaptors.gp2gp.gpc.DocumentToMHSTranslator;
import uk.nhs.adaptors.gp2gp.gpc.StorageDataWrapperProvider;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class SendAbsentAttachmentTaskExecutor implements TaskExecutor<SendAbsentAttachmentTaskDefinition> {

    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DocumentToMHSTranslator documentToMHSTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<SendAbsentAttachmentTaskDefinition> getTaskType() {
        return SendAbsentAttachmentTaskDefinition.class;
    }

    @Override
    public void execute(SendAbsentAttachmentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var messageId = taskDefinition.getMessageId();

        var fileContent = Base64Utils.toBase64String(AbsentAttachmentFileMapper.mapDataToAbsentAttachment(
            taskDefinition.getTitle(),
            taskDefinition.getToOdsCode(),
            taskDefinition.getConversationId()
        ));

        var fileName = buildAbsentAttachmentFileName(taskDefinition.getDocumentId());

        var mhsOutboundRequestData = documentToMHSTranslator.translateFileContentToMhsOutboundRequestData(taskDefinition, fileContent);

        var storageDataWrapperWithMhsOutboundRequest = StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, fileName);

        var ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, fileName, taskId, messageId, fileContent.getBytes(StandardCharsets.UTF_8).length
        );
        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
