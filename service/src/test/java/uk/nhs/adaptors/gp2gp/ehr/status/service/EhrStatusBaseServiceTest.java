package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ERROR;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ORIGINAL_FILE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.PLACEHOLDER;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.SKELETON_MESSAGE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;

@ExtendWith(MockitoExtension.class)
public class EhrStatusBaseServiceTest {

    private static final EhrExtractStatus.GpcDocument SKELETON_DOCUMENT = EhrExtractStatus.GpcDocument.builder()
        .isSkeleton(true)
        .build();

    private static final EhrExtractStatus.GpcDocument PLACEHOLDER_DOCUMENT_1 = EhrExtractStatus.GpcDocument.builder()
        .fileName("AbsentAttachmentTest.txt")
        .isSkeleton(false)
        .build();

    private static final EhrExtractStatus.GpcDocument PLACEHOLDER_DOCUMENT_2 = EhrExtractStatus.GpcDocument.builder()
        .objectName("AbsentAttachmentTest.txt")
        .isSkeleton(false)
        .build();

    private static final EhrExtractStatus.GpcDocument ORIGINAL_FILE_DOCUMENT = EhrExtractStatus.GpcDocument.builder()
        .fileName("test.txt")
        .objectName("test.txt")
        .isSkeleton(false)
        .sentToMhs(EhrExtractStatus.GpcAccessDocument.SentToMhs.builder()
            .messageId(List.of("123456"))
            .build())
        .build();

    private static final List<EhrExtractStatus.EhrReceivedAcknowledgement> ONE_FAILED_ACK_LIST = List.of(
        EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .messageRef("123456")
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                .code("99")
                .display("Test Error")
                .build()))
            .build()
    );

    @Mock
    private EhrExtractStatusRepository extractStatusRepository;
    @InjectMocks
    private EhrStatusBaseService ehrStatusBaseService;

    @Test
    public void When_GetFileStatus_WithSkeleton_Expect_SkeletonStatus() {
        FileStatus fileStatus = ehrStatusBaseService.getFileStatus(SKELETON_DOCUMENT, List.of());

        assertThat(fileStatus).isEqualTo(SKELETON_MESSAGE);
    }

    @Test
    public void When_GetFileStatus_WithAbsentAttachmentInFilename_Expect_PlaceholderStatus() {
        FileStatus fileStatus = ehrStatusBaseService.getFileStatus(PLACEHOLDER_DOCUMENT_1, List.of());

        assertThat(fileStatus).isEqualTo(PLACEHOLDER);

    }

    @Test
    public void When_GetFileStatus_WithAbsentAttachmentInObjectName_Expect_PlaceholderStatus() {
        FileStatus fileStatus = ehrStatusBaseService.getFileStatus(PLACEHOLDER_DOCUMENT_2, List.of());

        assertThat(fileStatus).isEqualTo(PLACEHOLDER);
    }

    @Test
    public void When_GetFileStatus_WithNoErrorOrPlaceholder_Expect_OriginalFileStatus() {
        FileStatus fileStatus = ehrStatusBaseService.getFileStatus(ORIGINAL_FILE_DOCUMENT, List.of());

        assertThat(fileStatus).isEqualTo(ORIGINAL_FILE);
    }

    @Test
    public void When_GetFileStatus_WithNACK_Expect_ErrorStatus() {
        FileStatus fileStatus = ehrStatusBaseService.getFileStatus(ORIGINAL_FILE_DOCUMENT, ONE_FAILED_ACK_LIST);

        assertThat(fileStatus).isEqualTo(ERROR);
    }



}
