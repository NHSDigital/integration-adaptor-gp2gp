package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ERROR;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ORIGINAL_FILE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.PLACEHOLDER;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.SKELETON_MESSAGE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_INCUMBENT;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_NME;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;

@ExtendWith(MockitoExtension.class)
public class EhrStatusServiceTest {

    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";

    private static final EhrExtractStatus COMPLETE_EHR_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().conversationClosed(Instant.now()).build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_NME_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AE").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AE").build())
        .error(EhrExtractStatus.Error.builder().code("18").occurredAt(Instant.now()).build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_INCUMBENT_EXTRACT_STATUS_1 = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                    .code("30")
                    .display("Test Error")
                    .build()))
            .build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus FAILED_INCUMBENT_EXTRACT_STATUS_2 = EhrExtractStatus.builder()
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .errors(List.of(EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                    .code("30")
                    .display("Test Error")
                    .build()))
            .build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

    private static final EhrExtractStatus IN_PROGRESS_EXTRACT_STATUS = EhrExtractStatus.builder()
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder().toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
        .build();

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
    private EhrStatusService ehrStatusService;

    @Test
    public void When_GetEhrStatus_WithCompleteMigration_Expect_CompleteStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(COMPLETE_EHR_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(COMPLETE);

    }

    @Test
    public void When_GetEhrStatus_WithFailedNME_Expect_FailedNameStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_NME_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_NME);
    }

    @Test
    public void When_GetEhrStatus_WithFailedIncumbent_Expect_FailedIncumbentStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_INCUMBENT_EXTRACT_STATUS_1));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    @Test
    public void When_GetEhrStatus_WithFailedIncumbentBeforeContinue_Expect_FailedIncumbentStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(FAILED_INCUMBENT_EXTRACT_STATUS_2));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(FAILED_INCUMBENT);
    }

    @Test
    public void When_GetEhrStatus_WithInProgress_Expect_InProgressStatus() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(IN_PROGRESS_EXTRACT_STATUS));

        Optional<EhrStatus> status = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        assertThat(status).isPresent();
        assertThat(status.get().getMigrationStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    public void When_GetFileStatus_WithSkeleton_Expect_SkeletonStatus() {
        FileStatus fileStatus = ehrStatusService.getFileStatus(SKELETON_DOCUMENT, List.of());

        assertThat(fileStatus).isEqualTo(SKELETON_MESSAGE);
    }

    @Test
    public void When_GetFileStatus_WithAbsentAttachmentInFilename_Expect_PlaceholderStatus() {
        FileStatus fileStatus = ehrStatusService.getFileStatus(PLACEHOLDER_DOCUMENT_1, List.of());

        assertThat(fileStatus).isEqualTo(PLACEHOLDER);

    }

    @Test
    public void When_GetFileStatus_WithAbsentAttachmentInObjectName_Expect_PlaceholderStatus() {
        FileStatus fileStatus = ehrStatusService.getFileStatus(PLACEHOLDER_DOCUMENT_2, List.of());

        assertThat(fileStatus).isEqualTo(PLACEHOLDER);
    }

    @Test
    public void When_GetFileStatus_WithNoErrorOrPlaceholder_Expect_OriginalFileStatus() {
        FileStatus fileStatus = ehrStatusService.getFileStatus(ORIGINAL_FILE_DOCUMENT, List.of());

        assertThat(fileStatus).isEqualTo(ORIGINAL_FILE);
    }

    @Test
    public void When_GetFileStatus_WithNACK_Expect_ErrorStatus() {
        FileStatus fileStatus = ehrStatusService.getFileStatus(ORIGINAL_FILE_DOCUMENT, ONE_FAILED_ACK_LIST);

        assertThat(fileStatus).isEqualTo(ERROR);
    }

    @Test
    public void When_GetFileStatus_Expect_AsidCodesArePresent() {
        when(extractStatusRepository.findByConversationId(any())).thenReturn(Optional.of(COMPLETE_EHR_EXTRACT_STATUS));

        Optional<EhrStatus> statusOptional = ehrStatusService.getEhrStatus(UUID.randomUUID().toString());

        EhrStatus status = statusOptional.orElseThrow();

        assertThat(status.getToAsid()).isEqualTo(TO_ASID_CODE);
        assertThat(status.getFromAsid()).isEqualTo(FROM_ASID_CODE);

    }

}
