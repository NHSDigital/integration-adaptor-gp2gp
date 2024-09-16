package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ERROR;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ORIGINAL_FILE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.PLACEHOLDER;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.SKELETON_MESSAGE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE_WITH_ISSUES;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_INCUMBENT;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_NME;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

@ExtendWith(MockitoExtension.class)
public class EhrStatusBaseServiceTest {

    public static final int TWENTY_DAYS = 20;
    public static final String ACK_TYPE = "AA";
    private static final String NACK_TYPE = "AE";
    private static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    public static final int NINE_DAYS = 9;
    public static final String DISPLAY_ERROR_MESSAGE = "The acknowledgement has been received after 8 days";
    public static final String ERROR_CODE = "99";

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

    @Test
    void evaluateMigrationStatusResolvesToFailedIncumbent() {
        var conversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = inProgressTransfers(conversationId);

        MigrationStatus migrationStatus = ehrStatusBaseService.evaluateMigrationStatus(ehrExtractStatus, List.of());

        assertEquals(FAILED_INCUMBENT, migrationStatus);
    }

    @Test
    void evaluateMigrationStatusResolvesToInProgress() {
        var conversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = inProgressTransfers(conversationId);
        ehrExtractStatus.setEhrReceivedAcknowledgement(null);

        MigrationStatus migrationStatus = ehrStatusBaseService.evaluateMigrationStatus(ehrExtractStatus, List.of());

        assertEquals(IN_PROGRESS, migrationStatus);
    }

    @Test
    void evaluateMigrationStatusResolvesToComplete() {
        var conversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = completeTransfers(conversationId);

        MigrationStatus migrationStatus = ehrStatusBaseService.evaluateMigrationStatus(ehrExtractStatus, List.of());

        assertEquals(COMPLETE, migrationStatus);
    }

    @Test
    void evaluateMigrationStatusResolvesToCompleteWithIssues() {
        var conversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = completeTransfers(conversationId);

        List<EhrStatus.AttachmentStatus> attachmentStatusList = new ArrayList<>();
        attachmentStatusList.add(
            EhrStatus.AttachmentStatus.builder().fileStatus(PLACEHOLDER).build());

        MigrationStatus migrationStatus = ehrStatusBaseService.evaluateMigrationStatus(ehrExtractStatus, attachmentStatusList);

        assertEquals(COMPLETE_WITH_ISSUES, migrationStatus);
    }

    @Test
    void evaluateMigrationStatusResolvesToFailedNme() {
        var conversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = inProgressTransfers(conversationId);
        ehrExtractStatus.getAckPending().setTypeCode(NACK_TYPE);
        ehrExtractStatus.setError(EhrExtractStatus.Error.builder().build());

        MigrationStatus migrationStatus = ehrStatusBaseService.evaluateMigrationStatus(ehrExtractStatus, List.of());

        assertEquals(FAILED_NME, migrationStatus);
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    private EhrExtractStatus inProgressTransfers(String conversationId) {
        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackPending(buildPositiveAckPending())
            .ackToRequester(buildPositiveAckToRequester())
            .conversationId(conversationId)
            .created(Instant.now().minus(Duration.ofDays(TWENTY_DAYS)))
            .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                                .sentAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                .build())
            .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                       .sentAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                       .taskId(generateRandomUppercaseUUID())
                                       .build())
            .ehrExtractMessageId(generateRandomUppercaseUUID())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                                   .documents(List.of())
                                   .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                                     .accessedAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                     .objectName(generateRandomUppercaseUUID() + ".json")
                                     .taskId(generateRandomUppercaseUUID())
                                     .build())
            .messageTimestamp(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
            .updatedAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
            .ehrReceivedAcknowledgement(
                EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(List.of(
                    EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails
                        .builder()
                        .code(ERROR_CODE)
                        .display(DISPLAY_ERROR_MESSAGE)
                        .build())).build())
            .build();

        return extractStatus;
    }

    private EhrExtractStatus completeTransfers(String conversationId) {
        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackPending(buildPositiveAckPending())
            .ackToRequester(buildPositiveAckToRequester())
            .conversationId(conversationId)
            .created(Instant.now().minus(Duration.ofDays(TWENTY_DAYS)))
            .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                                .sentAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                .build())
            .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                       .sentAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                       .taskId(generateRandomUppercaseUUID())
                                       .build())
            .ehrExtractMessageId(generateRandomUppercaseUUID())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                                   .documents(List.of())
                                   .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                                     .accessedAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
                                     .objectName(generateRandomUppercaseUUID() + ".json")
                                     .taskId(generateRandomUppercaseUUID())
                                     .build())
            .messageTimestamp(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
            .updatedAt(Instant.now().minus(Duration.ofDays(NINE_DAYS)))
            .ehrReceivedAcknowledgement(
                EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                    .conversationClosed(FIVE_DAYS_AGO)
                    .build())
            .build();

        return extractStatus;
    }

    private EhrExtractStatus.AckPending buildPositiveAckPending() {
        return EhrExtractStatus.AckPending.builder()
            .messageId(generateRandomUppercaseUUID())
            .taskId(generateRandomUppercaseUUID())
            .typeCode(ACK_TYPE)
            .updatedAt(FIVE_DAYS_AGO.toString())
            .build();
    }

    private EhrExtractStatus.AckToRequester buildPositiveAckToRequester() {
        return EhrExtractStatus.AckToRequester.builder()
            .detail(null)
            .messageId(generateRandomUppercaseUUID())
            .reasonCode(null)
            .taskId(generateRandomUppercaseUUID())
            .typeCode(ACK_TYPE)
            .build();
    }

    private EhrExtractStatus.EhrRequest buildEhrRequest() {
        return EhrExtractStatus.EhrRequest.builder()
            .messageId(generateRandomUppercaseUUID())
            .build();
    }



}
