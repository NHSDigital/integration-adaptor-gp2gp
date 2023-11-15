package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.ACK_TYPE;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({MongoDBExtension.class})
@DirtiesContext
@SpringBootTest
public class EhrExtractStatusServiceTest {

    private static final Instant FIVE_DAYS_AGO = Instant.now().minus(Duration.ofDays(5));

    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @BeforeEach
    public void emptyDatabase() {
        ehrExtractStatusRepository.deleteAll();
    }

    @Test
    public void When_FindInProgressTransfers_With_OneInProgress_Expect_Returned() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(inProgressConversationId);

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.size()).isOne();
        assertThat(results.get(0).getConversationId()).isEqualTo(inProgressConversationId);
    }

    @Test
    public void When_FindInProgressTransfers_With_MixedTransfers_Expect_InProgressFound() {
        var inProgressConversationId = generateRandomUppercaseUUID();

        addInProgressTransfer(inProgressConversationId);
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
            addInProgressTransfer(inProgressConversationId);
        }

        List<EhrExtractStatus> results = ehrExtractStatusService.findInProgressTransfers();

        assertThat(results.size()).isEqualTo(inProgressConversationIds.size());

        var returnedConversationIds = results.stream()
            .map(EhrExtractStatus::getConversationId)
            .collect(Collectors.toList());

        assertThat(returnedConversationIds).isEqualTo(inProgressConversationIds);
    }

    private void addInProgressTransfer(String conversationId) {
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

    public void addCompleteTransfer() {
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

        ehrExtractStatusRepository.save(extractStatus);

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
