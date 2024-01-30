package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.ACK_TYPE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_ASID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_ODS_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_PARTY_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_DISPLAY;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.NHS_NUMBER;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.NME_NACK_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.NME_NACK_DISPLAY;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.NME_NACK_TYPE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.TO_ASID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.TO_ODS_CODE;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({MongoDBExtension.class})
@DirtiesContext
@SpringBootTest
public class EhrExtractStatusServiceTest {

    public static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));

    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @MockBean
    private TimestampService timestampService;

    @BeforeEach
    public void emptyDatabase() {
        ehrExtractStatusRepository.deleteAll();
    }

    @Test
    public void When_FetchDocumentObjectNameAndSize_With_OneMissingAttachment_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        ArrayList<EhrExtractStatus.GpcDocument> documents = new ArrayList<>();

        EhrExtractStatus.GpcDocument document = EhrExtractStatus.GpcDocument.builder()
                        .fileName("AbsentAttachment4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx")
                        .documentId("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60")
                        .contentLength(244)
                        .gpConnectErrorMessage("404 Not Found")
                        .contentType("application/msword")
                        .build();

        documents.add(document);
        addInProgressTransfer(inProgressConversationId, documents);

        Map<String, String> results = ehrExtractStatusService.fetchDocumentObjectNameAndSize(inProgressConversationId);

        Map<String, String> expectedResult = new HashMap<>();

        expectedResult.put("FILENAME_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "AbsentAttachment4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx");
        expectedResult.put("LENGTH_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "244");
        expectedResult.put("ERROR_MESSAGE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "Absent Attachment: 404 Not Found");
        expectedResult.put("CONTENT_TYPE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "text/plain");

        assertThat(results.size()).isEqualTo(4);
        assertThat(results).isEqualTo(expectedResult);
    }

    @Test
    public void When_FetchDocumentObjectNameAndSize_With_OnePresentAttachment_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        ArrayList<EhrExtractStatus.GpcDocument> documents = new ArrayList<>();

        EhrExtractStatus.GpcDocument document = EhrExtractStatus.GpcDocument.builder()
                .fileName("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx")
                .documentId("4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60")
                .contentLength(244)
                .gpConnectErrorMessage(null)
                .contentType("application/msword")
                .build();

        documents.add(document);
        addInProgressTransfer(inProgressConversationId, documents);

        Map<String, String> results = ehrExtractStatusService.fetchDocumentObjectNameAndSize(inProgressConversationId);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("FILENAME_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60.rtx");
        expectedResult.put("LENGTH_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "244");
        expectedResult.put("ERROR_MESSAGE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "");
        expectedResult.put("CONTENT_TYPE_PLACEHOLDER_ID=4E0C8345-A9AB-48EA-8882-DC9E9F3F5F60", "application/msword");

        assertThat(results.size()).isEqualTo(4);
        assertThat(results).isEqualTo(expectedResult);
    }

    @Test
    public void When_FindInProgressTransfers_With_OneInProgress_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(inProgressConversationId, new ArrayList<>());

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.size()).isOne();
        assertThat(results.get(0).getConversationId()).isEqualTo(inProgressConversationId);
    }

    @Test
    public void When_FindInProgressTransfers_With_MixedTransfers_Expect_InProgressFound() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(inProgressConversationId, new ArrayList<>());
        addCompleteTransfer();
        addFailedIncumbentTransfer();
        addFailedNmeTransfer();

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.size()).isOne();

        assertThat(results.get(0).getConversationId()).isEqualTo(inProgressConversationId);
    }

    @Test
    public void When_FindInProgressTransfers_With_AllFailedOrComplete_Expect_EmptyList() {
        addCompleteTransfer();
        addCompleteTransfer();
        addFailedIncumbentTransfer();
        addFailedNmeTransfer();
        addFailedIncumbentTransfer();

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.isEmpty()).isTrue();
    }

    @Test
    public void When_FindInProgressTransfers_With_MultipleInProgress_Expect_AllReturned() {
        var inProgressConversationIds = List.of(
            generateRandomUppercaseUUID(),
            generateRandomUppercaseUUID(),
            generateRandomUppercaseUUID()
        );

        addFailedIncumbentTransfer();

        for (String inProgressConversationId : inProgressConversationIds) {
            addInProgressTransfer(inProgressConversationId, new ArrayList<>());
        }

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.size()).isEqualTo(inProgressConversationIds.size());

        var returnedConversationIds = results.stream()
            .map(EhrExtractStatus::getConversationId)
            .collect(Collectors.toList());

        assertThat(returnedConversationIds).isEqualTo(inProgressConversationIds);
    }

    @Test
    public void When_UpdateEhrExtractStatusAccessDocument_Expect_DocumentRecordUpdated() {
        when(timestampService.now()).thenReturn(NOW);
        var ehrStatus = addCompleteTransferWithDocuments();

        updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), DOCUMENT_ID);

        EhrExtractStatus.GpcDocument actual = ehrExtractStatusRepository.findByConversationId(ehrStatus.getConversationId()).orElseThrow()
                .getGpcAccessDocument().getDocuments().get(0);
        assertAll(
            () -> assertThat(actual.getAccessedAt()).isEqualTo(NOW.truncatedTo(ChronoUnit.MILLIS)),
            () -> assertThat(actual.getTaskId()).isEqualTo("80010"),
            () -> assertThat(actual.getObjectName()).isEqualTo("this is a storage path.path"),
            () -> assertThat(actual.getMessageId()).isEqualTo("988290"),
            () -> assertThat(actual.getContentLength()).isEqualTo(1),
            () -> assertThat(actual.getGpConnectErrorMessage()).isEqualTo("This is a fantastic error message")
        );
    }

    private EhrExtractStatus updateEhrExtractStatusAccessDocument(String conversationId, String documentId) {
        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            GetGpcDocumentTaskDefinition.builder()
                .conversationId(conversationId)
                .documentId(documentId)
                .taskId("80010")
                .messageId("988290")
                .build(),
            "this is a storage path.path",
            1,
            "This is a fantastic error message"
        );
    }

    @Test
    public void When_UpdateEhrExtractStatusAccessDocument_With_InvalidConversationId_Expect_ThrowsException() {
        addCompleteTransferWithDocuments();

        assertThrows(
            EhrExtractException.class,
            () -> updateEhrExtractStatusAccessDocument("I AM NOT A VALID CONVERSATION ID", DOCUMENT_ID)
        );
    }

    @Test
    public void When_UpdateEhrExtractStatusAccessDocument_With_InvalidDocumentId_Expect_ThrowsException() {
        final var ehrStatus = addCompleteTransferWithDocuments();

        assertThrows(
            EhrExtractException.class,
            () -> updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), "I AM NOT A VALID DOCUMENT ID")
        );
    }


    @Test
    public void When_UpdateEhrExtractStatusAccessDocument_Expect_ReturnsUpdatedEhrStatusRecord() {
        when(timestampService.now()).thenReturn(NOW);
        var ehrStatus = addCompleteTransferWithDocuments();

        final var returnedRecord = updateEhrExtractStatusAccessDocument(ehrStatus.getConversationId(), DOCUMENT_ID);

        assertThat(returnedRecord).usingRecursiveComparison().isEqualTo(
            ehrExtractStatusRepository.findByConversationId(ehrStatus.getConversationId()).orElseThrow()
        );
    }

    @Test
    public void When_UpdateEhrExtractStatusAccessDocumentDocumentReferences_Expect_DocumentAddedToMongoDb() {
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

    private void addInProgressTransfer(String conversationId, ArrayList<EhrExtractStatus.GpcDocument> documents) {
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
                .objectName(generateRandomUppercaseUUID() + ".json")
                .taskId(generateRandomUppercaseUUID())
                .build())
            .messageTimestamp(FIVE_DAYS_AGO)
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        ehrExtractStatusRepository.save(extractStatus);
    }

    private void addFailedNmeTransfer() {
        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackPending(EhrExtractStatus.AckPending.builder()
                .messageId(generateRandomUppercaseUUID())
                .taskId(generateRandomUppercaseUUID())
                .typeCode(NME_NACK_TYPE)
                .updatedAt(FIVE_DAYS_AGO.toString())
                .build())
            .ackToRequester(EhrExtractStatus.AckToRequester.builder()
                .detail(NME_NACK_DISPLAY)
                .messageId(generateRandomUppercaseUUID())
                .reasonCode(NME_NACK_CODE)
                .taskId(generateRandomUppercaseUUID())
                .typeCode(NME_NACK_TYPE)
                .build())
            .conversationId(generateRandomUppercaseUUID())
            .created(FIVE_DAYS_AGO)
            .ehrRequest(buildEhrRequest())
            .error(EhrExtractStatus.Error.builder()
                .code(NME_NACK_CODE)
                .message(NME_NACK_DISPLAY)
                .occurredAt(FIVE_DAYS_AGO)
                .taskType("GET_GPC_STRUCTURED")
                .build())
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        ehrExtractStatusRepository.save(extractStatus);
    }

    private void addFailedIncumbentTransfer() {
        String ehrMessageRef = generateRandomUppercaseUUID();

        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackHistory(EhrExtractStatus.AckHistory.builder()
                .acks(List.of(
                    EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                        .rootId(generateRandomUppercaseUUID())
                        .received(FIVE_DAYS_AGO)
                        .conversationClosed(FIVE_DAYS_AGO)
                        .errors(List.of(
                            EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                                .code(INCUMBENT_NACK_CODE)
                                .display(INCUMBENT_NACK_DISPLAY)
                                .build()))
                        .messageRef(ehrMessageRef)
                        .build()))
                .build())
            .ackPending(buildPositiveAckPending())
            .ackToRequester(buildPositiveAckToRequester())
            .conversationId(generateRandomUppercaseUUID())
            .created(FIVE_DAYS_AGO)
            .ehrExtractCore(EhrExtractStatus.EhrExtractCore.builder()
                .sentAt(FIVE_DAYS_AGO)
                .taskId(generateRandomUppercaseUUID())
                .build())
            .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                .sentAt(FIVE_DAYS_AGO)
                .taskId(generateRandomUppercaseUUID())
                .build())
            .ehrExtractMessageId(generateRandomUppercaseUUID())
            .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                .conversationClosed(FIVE_DAYS_AGO)
                .errors(List.of(
                    EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                        .code(INCUMBENT_NACK_CODE)
                        .display(INCUMBENT_NACK_DISPLAY)
                        .build()))
                .messageRef(ehrMessageRef)
                .received(FIVE_DAYS_AGO)
                .rootId(generateRandomUppercaseUUID())
                .build())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                .documents(new ArrayList<>())
                .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                .accessedAt(FIVE_DAYS_AGO)
                .objectName(generateRandomUppercaseUUID() + ".json")
                .taskId(generateRandomUppercaseUUID())
                .build())
            .messageTimestamp(FIVE_DAYS_AGO)
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        ehrExtractStatusRepository.save(extractStatus);
    }

    public EhrExtractStatus addCompleteTransfer() {
        String ehrMessageRef = generateRandomUppercaseUUID();

        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
            .ackHistory(EhrExtractStatus.AckHistory.builder()
                .acks(List.of(
                    EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                        .rootId(generateRandomUppercaseUUID())
                        .received(FIVE_DAYS_AGO)
                        .conversationClosed(FIVE_DAYS_AGO)
                        .messageRef(ehrMessageRef)
                        .build()))
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
            .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                .conversationClosed(FIVE_DAYS_AGO)
                .messageRef(ehrMessageRef)
                .received(FIVE_DAYS_AGO)
                .rootId(generateRandomUppercaseUUID())
                .build())
            .ehrRequest(buildEhrRequest())
            .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                .documents(new ArrayList<>())
                .build())
            .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                .accessedAt(FIVE_DAYS_AGO)
                .objectName(generateRandomUppercaseUUID() + ".json")
                .taskId(generateRandomUppercaseUUID())
                .build())
            .messageTimestamp(FIVE_DAYS_AGO)
            .updatedAt(FIVE_DAYS_AGO)
            .build();

        return ehrExtractStatusRepository.save(extractStatus);
    }


    public EhrExtractStatus addCompleteTransferWithDocuments() {
        String ehrMessageRef = generateRandomUppercaseUUID();

        EhrExtractStatus extractStatus = EhrExtractStatus.builder()
                .ackHistory(EhrExtractStatus.AckHistory.builder()
                        .acks(List.of(
                                EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                                        .rootId(generateRandomUppercaseUUID())
                                        .received(FIVE_DAYS_AGO)
                                        .conversationClosed(FIVE_DAYS_AGO)
                                        .messageRef(ehrMessageRef)
                                        .build()))
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
                .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                        .conversationClosed(FIVE_DAYS_AGO)
                        .messageRef(ehrMessageRef)
                        .received(FIVE_DAYS_AGO)
                        .rootId(generateRandomUppercaseUUID())
                        .build())
                .ehrRequest(buildEhrRequest())
                .gpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                        .documents(List.of(
                            EhrExtractStatus.GpcDocument.builder().documentId(DOCUMENT_ID).build()
                        ))
                        .build())
                .gpcAccessStructured(EhrExtractStatus.GpcAccessStructured.builder()
                        .accessedAt(FIVE_DAYS_AGO)
                        .objectName(generateRandomUppercaseUUID() + ".json")
                        .taskId(generateRandomUppercaseUUID())
                        .build())
                .messageTimestamp(FIVE_DAYS_AGO)
                .updatedAt(FIVE_DAYS_AGO)
                .build();

        return ehrExtractStatusRepository.save(extractStatus);
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
