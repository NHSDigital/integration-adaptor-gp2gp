package uk.nhs.adaptors.gp2gp.ehr;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AbsentAttachmentFileMapper;
import uk.nhs.adaptors.gp2gp.gpc.DetectTranslationCompleteService;
import uk.nhs.adaptors.gp2gp.gpc.DocumentToMHSTranslator;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetAbsentAttachmentTaskExecutorTest {
    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @Mock
    private DocumentToMHSTranslator documentToMHSTranslator;

    @Mock
    private DetectTranslationCompleteService detectTranslationCompleteService;

    @Mock
    private StorageConnectorService storageConnectorService;

    @Mock
    private AbsentAttachmentFileMapper absentAttachmentFileMapper;

    @InjectMocks
    private GetAbsentAttachmentTaskExecutor getAbsentAttachmentTaskExecutor;

    @Test
    public void When_HandleAbsentAttachmentWithNoError_Expect_DefaultValueIsUsedAsError() {
        var taskDefinition = buildAbsentAttachment(null);
        when(absentAttachmentFileMapper.mapFileDataToAbsentAttachment(anyString(), anyString(), anyString()))
            .thenReturn("file content");

        getAbsentAttachmentTaskExecutor.handleAbsentAttachment(taskDefinition,Optional.empty());

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocument(
            any(),
            any(),
            anyInt(),
            eq("The document could not be retrieved"),
            any()
        );
    }

    @Test
    public void When_HandleAbsentAttachmentWithJustTaskDefinitionTitle_Expect_TitleIsUsedAsError() {
        var taskDefinition = buildAbsentAttachment("This-is-the-task-definition-title");
        when(absentAttachmentFileMapper.mapFileDataToAbsentAttachment(anyString(), anyString(), anyString()))
            .thenReturn("file content");

        getAbsentAttachmentTaskExecutor.handleAbsentAttachment(
            taskDefinition,
            Optional.empty());

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocument(
            any(),
            any(),
            anyInt(),
            eq("This-is-the-task-definition-title"),
            any()
        );
    }

    @Test
    public void When_HandleAbsentAttachmentWithJustGpcResponseError_Expect_GpcResponseErrorIsUsedAsError() {
        var taskDefinition = buildAbsentAttachment(null);
        when(absentAttachmentFileMapper.mapFileDataToAbsentAttachment(anyString(), anyString(), anyString()))
            .thenReturn("file content");

        getAbsentAttachmentTaskExecutor.handleAbsentAttachment(
            taskDefinition,
            Optional.of("This-is-the-gpc-response-error"));

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocument(
            any(),
            any(),
            anyInt(),
            eq("This-is-the-gpc-response-error"),
            any()
        );
    }

    @Test
    public void When_HandleAbsentAttachmentWithGpcResponseErrorAndTitle_Expect_GpcResponseErrorIsUsedAsError() {
        var taskDefinition = buildAbsentAttachment("This-is-the-task-definition-title");
        when(absentAttachmentFileMapper.mapFileDataToAbsentAttachment(anyString(), anyString(), anyString()))
            .thenReturn("file content");

        getAbsentAttachmentTaskExecutor.handleAbsentAttachment(
            taskDefinition,
            Optional.of("This-is-the-gpc-response-error"));

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocument(
            any(),
            any(),
            anyInt(),
            eq("This-is-the-gpc-response-error"),
            any()
        );
    }

    @Test
    public void When_HandleAbsentAttachment_Expect_AbsentAttachmentFilenameIsUsed() {
        var taskDefinition = buildAbsentAttachment(null);
        when(absentAttachmentFileMapper.mapFileDataToAbsentAttachment(anyString(), anyString(), anyString()))
            .thenReturn("file content");

        getAbsentAttachmentTaskExecutor.handleAbsentAttachment(taskDefinition, Optional.empty());

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocument(
            any(),
            eq("AbsentAttachmentDocument-Id.txt"),
            anyInt(),
            any(),
            eq("AbsentAttachmentDocument-Id.txt")
        );
    }

    private static GetAbsentAttachmentTaskDefinition buildAbsentAttachment(@Nullable String title) {
        return GetAbsentAttachmentTaskDefinition.builder()
            .conversationId("Conversation-Id")
            .taskId("Task-Id")
            .originalDescription("This-is-the-original-description")
            .toOdsCode("XX1111")
            .documentId("Document-Id")
            .title(title)
            .build();
    }
}
