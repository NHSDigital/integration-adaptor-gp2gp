package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.ACK_TYPE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_ASID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_ODS_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_PARTY_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.NHS_NUMBER;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.TO_ASID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.TO_ODS_CODE;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.scheduling.EhrExtractTimeoutScheduler;
import uk.nhs.adaptors.gp2gp.ehr.utils.ErrorDetail;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({MongoDBExtension.class})
@DirtiesContext
@SpringBootTest
public class EhrExtractStatusServiceIT {

    private static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    private static final Instant TEN_DAYS_AGO = NOW.minus(Duration.ofDays(10));
    private static final int DEFAULT_CONTENT_LENGTH = 244;
    private static final String CONTENT_TYPE_MSWORD = "application/msword";
    public static final String JSON_SUFFIX = ".json";
    private static final ErrorDetail ACK_TIMEOUT_ERROR = ErrorDetail.ACK_TIMEOUT;

    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private EhrExtractTimeoutScheduler ehrExtractTimeoutScheduler;

    @MockBean
    private TimestampService timestampService;

    @MockBean
    private Logger logger;

    @BeforeEach
    public void emptyDatabase() {
        ehrExtractStatusRepository.deleteAll();
    }

    @Test
    void When_EhrStatusWithExceededTimeout_Expect_EhrStatusShouldNotBeUpdated() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        var ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);

        addInProgressTransferWithExceededAckTimeout(inProgressConversationId, List.of());

        ehrExtractTimeoutScheduler.processEhrExtractAckTimeouts();
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        var ehrReceivedAcknowledgement = getEhrReceivedAcknowledgement(inProgressConversationId);
        ehrReceivedAcknowledgement.setReceived(NOW);
        ehrReceivedAcknowledgement.setConversationClosed(NOW);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(inProgressConversationId, ehrReceivedAcknowledgement);

        verify(logger, times(1))
            .warn("Received an ACK message with conversation_id: {}, "
                  + "but it is being ignored because the EhrExtract has already been marked as failed "
                  + "from not receiving an acknowledgement from the requester in time.",
                  inProgressConversationId);
    }

    @Test
    void When_FetchDocumentObjectNameAndSize_With_OneMissingAttachment_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(
            inProgressConversationId, List.of(
                EhrExtractStatus.GpcDocument.builder()
                    .fileName("AbsentAttachment4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx")
                    .documentId("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60")
                    .contentLength(DEFAULT_CONTENT_LENGTH)
                    .gpConnectErrorMessage("404 Not Found")
                    .contentType(CONTENT_TYPE_MSWORD)
                    .build()
            )
        );

        final var results = ehrExtractStatusService.fetchDocumentObjectNameAndSize(inProgressConversationId);

        assertThat(results).isEqualTo(Map.of(
            "FILENAME_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "AbsentAttachment4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx",
            "LENGTH_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "244",
            "ERROR_MESSAGE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "Absent Attachment: 404 Not Found ",
            "CONTENT_TYPE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "text/plain"
        ));
    }

    @Test
    void When_FetchDocumentObjectNameAndSize_With_OnePresentAttachment_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(
            inProgressConversationId, List.of(
                EhrExtractStatus.GpcDocument.builder()
                    .fileName("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx")
                    .documentId("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60")
                    .contentLength(DEFAULT_CONTENT_LENGTH)
                    .gpConnectErrorMessage(null)
                    .contentType(CONTENT_TYPE_MSWORD)
                    .build()
            )
        );

        final var results = ehrExtractStatusService.fetchDocumentObjectNameAndSize(inProgressConversationId);

        assertThat(results).isEqualTo(Map.of(
            "FILENAME_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx",
            "LENGTH_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", Integer.toString(DEFAULT_CONTENT_LENGTH),
            "ERROR_MESSAGE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "",
            "CONTENT_TYPE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", CONTENT_TYPE_MSWORD
        ));
    }

    @Test
    void When_FetchDocumentObjectNameAndSize_With_InvalidConversation_Expect_EmptyMap() {
        final var fakeConversationId = generateRandomUppercaseUUID();

        assertThat(ehrExtractStatusService.fetchDocumentObjectNameAndSize(fakeConversationId)).isEqualTo(Collections.EMPTY_MAP);
    }

    @Test
    void When_UpdateEhrExtractStatusAccessDocument_Expect_DocumentRecordUpdated() {
        when(timestampService.now()).thenReturn(NOW);
        var ehrStatus = addCompleteTransferWithDocument();

        updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), DOCUMENT_ID);

        EhrExtractStatus.GpcDocument actual = ehrExtractStatusRepository.findByConversationId(ehrStatus.getConversationId()).orElseThrow()
                .getGpcAccessDocument().getDocuments().get(0);
        assertAll(
            () -> assertThat(actual.getAccessedAt()).isEqualTo(NOW.truncatedTo(ChronoUnit.MILLIS)),
            () -> assertThat(actual.getTaskId()).isEqualTo("80010"),
            () -> assertThat(actual.getObjectName()).isEqualTo("this is a storage path.path"),
            () -> assertThat(actual.getMessageId()).isEqualTo("988290"),
            () -> assertThat(actual.getContentLength()).isEqualTo(1),
            () -> assertThat(actual.getGpConnectErrorMessage()).isEqualTo("This is a fantastic error message"),
            () -> assertThat(actual.getFileName()).isEqualTo("NewUpdatedFileName.txt")
        );
    }

    @Test
    void When_UpdateEhrExtractStatusAccessDocument_With_InvalidConversationId_Expect_ThrowsException() {
        addCompleteTransferWithDocument();

        assertThrows(
            EhrExtractException.class,
            () -> updateEhrExtractStatusAccessDocument("I AM NOT A VALID CONVERSATION ID", DOCUMENT_ID)
        );
    }

    @Test
    void When_UpdateEhrExtractStatusAccessDocument_With_InvalidDocumentId_Expect_ThrowsException() {
        final var ehrStatus = addCompleteTransferWithDocument();

        assertThrows(
            EhrExtractException.class,
            () -> updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), "I AM NOT A VALID DOCUMENT ID")
        );
    }


    @Test
    void When_UpdateEhrExtractStatusAccessDocument_Expect_ReturnsUpdatedEhrStatusRecord() {
        when(timestampService.now()).thenReturn(NOW);
        var ehrStatus = addCompleteTransferWithDocument();

        final var returnedRecord = updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), DOCUMENT_ID);

        assertThat(returnedRecord).usingRecursiveComparison().isEqualTo(
            ehrExtractStatusRepository.findByConversationId(ehrStatus.getConversationId()).orElseThrow()
        );
    }

    @Test
    void When_UpdateEhrExtractStatusAccessDocumentDocumentReferences_Expect_DocumentAddedToMongoDb() {
        var ehrStatus = addCompleteTransfer();

        ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
            ehrStatus.getConversationId(), List.of(EhrExtractStatus.GpcDocument.builder()
                .documentId("f368d574-b2aa-4255-9d98-97cca1d3502e").build()));

        assertThat(ehrExtractStatusRepository.findByConversationId(
            ehrStatus.getConversationId()).orElseThrow().getGpcAccessDocument().getDocuments().size())
            .isEqualTo(1);

        ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(
            ehrStatus.getConversationId(), List.of(EhrExtractStatus.GpcDocument.builder()
                .documentId("f368d574-b2aa-4255-9d98-97cca1d3502b").build()));

        assertThat(ehrExtractStatusRepository.findByConversationId(
            ehrStatus.getConversationId()).orElseThrow().getGpcAccessDocument().getDocuments().size())
            .isEqualTo(2);
    }

    EhrExtractStatus updateEhrExtractStatusAccessDocument(String conversationId, String documentId) {
        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            GetGpcDocumentTaskDefinition.builder()
                .conversationId(conversationId)
                .documentId(documentId)
                .taskId("80010")
                .messageId("988290")
                .build(),
            "this is a storage path.path",
            1,
            "This is a fantastic error message",
            "NewUpdatedFileName.txt"
                                                                           );
    }

    private void addInProgressTransfer(String conversationId, List<EhrExtractStatus.GpcDocument> documents) {
        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackPending(buildPositiveAckPending())
            .ackToRequester(buildPositiveAckToRequester())
            .conversationId(conversationId)
            .created(FIVE_DAYS_AGO)
            .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                .sentAt(FIVE_DAYS_AGO)
                .build())
            .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                .sentAt(FIVE_DAYS_AGO)
                .taskId(generateRandomUppercaseUUID())
                .build())
            .ehrExtractMessageId(generateRandomUppercaseUUID())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                .documents(documents)
                .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                .accessedAt(FIVE_DAYS_AGO)
                .objectName(generateRandomUppercaseUUIDWithJsonSuffix())
                .taskId(generateRandomUppercaseUUID())
                .build())
            .messageTimestamp(FIVE_DAYS_AGO)
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        ehrExtractStatusRepository.save(extractStatus);
    }

    private void addInProgressTransferWithExceededAckTimeout(String conversationId, List<EhrExtractStatus.GpcDocument> documents) {
        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackPending(buildPositiveAckPending())
            .ackToRequester(buildPositiveAckToRequester())
            .conversationId(conversationId)
            .created(FIVE_DAYS_AGO)
            .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                                .sentAt(FIVE_DAYS_AGO)
                                .build())
            .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                       .sentAt(TEN_DAYS_AGO)
                                       .taskId(generateRandomUppercaseUUID())
                                       .build())
            .ehrExtractMessageId(generateRandomUppercaseUUID())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                                   .documents(documents)
                                   .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                                     .accessedAt(FIVE_DAYS_AGO)
                                     .objectName(generateRandomUppercaseUUIDWithJsonSuffix())
                                     .taskId(generateRandomUppercaseUUID())
                                     .build())
            .messageTimestamp(FIVE_DAYS_AGO)
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        ehrExtractStatusRepository.save(extractStatus);
    }

    private EhrExtractStatus addCompleteTransfer() {
        return addCompleteTransferWithDocuments(List.of());
    }

    private @NotNull String generateRandomUppercaseUUIDWithJsonSuffix() {
        return generateRandomUppercaseUUID() + JSON_SUFFIX;
    }

    private EhrExtractStatus addCompleteTransferWithDocument() {
        return addCompleteTransferWithDocuments(List.of(
            EhrExtractStatus.GpcDocument.builder().documentId(DOCUMENT_ID).build()
        ));
    }

    private @NotNull EhrExtractStatus addCompleteTransferWithDocuments(List<EhrExtractStatus.GpcDocument> documents) {
        String ehrMessageRef = generateRandomUppercaseUUID();

        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
                .ackHistory(EhrExtractStatus.AckHistory.builder()
                        .acks(Collections.singletonList(getEhrReceivedAcknowledgement(ehrMessageRef)))
                        .build())
                .ackPending(EhrExtractStatus.AckPending.builder()
                        .messageId(generateRandomUppercaseUUID())
                        .taskId(generateRandomUppercaseUUID())
                        .typeCode(ACK_TYPE)
                        .updatedAt(FIVE_DAYS_AGO.toString())
                        .build())
                .ackToRequester(buildPositiveAckToRequester())
                .conversationId(generateRandomUppercaseUUID())
                .created(FIVE_DAYS_AGO)
                .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                        .sentAt(FIVE_DAYS_AGO)
                        .build())
                .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                        .sentAt(FIVE_DAYS_AGO)
                        .taskId(generateRandomUppercaseUUID())
                        .build())
                .ehrReceivedAcknowledgement(getEhrReceivedAcknowledgement(ehrMessageRef))
                .ehrRequest(buildEhrRequest())
                .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                        .documents(documents)
                        .build())
                .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                        .accessedAt(FIVE_DAYS_AGO)
                        .objectName(generateRandomUppercaseUUIDWithJsonSuffix())
                        .taskId(generateRandomUppercaseUUID())
                        .build())
                .messageTimestamp(FIVE_DAYS_AGO)
                .updatedAt(FIVE_DAYS_AGO)
                .build();

        return ehrExtractStatusRepository.save(extractStatus);
    }

    private EhrExtractStatus.EhrReceivedAcknowledgement getEhrReceivedAcknowledgement(String ehrMessageRef) {
        return EhrExtractStatus.EhrReceivedAcknowledgement.builder()
            .rootId(generateRandomUppercaseUUID())
            .received(FIVE_DAYS_AGO)
            .conversationClosed(FIVE_DAYS_AGO)
            .messageRef(ehrMessageRef)
            .build();
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
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
            .requestId(generateRandomUppercaseUUID())
            .nhsNumber(NHS_NUMBER)
            .fromPartyId(FROM_PARTY_ID)
            .fromAsid(FROM_ASID)
            .toAsid(TO_ASID)
            .toOdsCode(TO_ODS_CODE)
            .fromOdsCode(FROM_ODS_CODE)
            .messageId(generateRandomUppercaseUUID())
            .build();
    }
}
