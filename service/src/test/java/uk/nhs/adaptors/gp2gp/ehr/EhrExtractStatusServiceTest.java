package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import java.time.Duration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrExtractStatusServiceTest {

    private static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    public static final int NINE_DAYS = 9;
    public static final int EIGHT_DAYS = 8;
    public static final String ERROR_CODE = "99";
    public static final String ALTERNATIVE_ERROR_CODE = "26";
    public static final String ERROR_MESSAGE = "The acknowledgement has been received after 8 days";
    public static final String ACK_TYPE = "AA";
    public static final int TWENTY_DAYS = 20;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private Logger logger;

    @Mock
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Mock
    private TimestampService timestampService;

    @Mock
    private EhrExtractStatus.EhrReceivedAcknowledgement ack;

    @InjectMocks
    private EhrExtractStatusService ehrExtractStatusService;

    @BeforeEach
    void setUp() {
        ehrExtractStatusService = new EhrExtractStatusService(mongoTemplate, ehrExtractStatusRepository, timestampService);
    }

    @Test
    public void shouldUpdateStatusWithErrorWhenEhrExtractAckTimeoutOccurs() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, times(1))
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(eq(List.of(ehrExtractStatus)),
                                                                           eq(ERROR_CODE),
                                                                           eq(ERROR_MESSAGE));
    }

    @Test
    public void shouldNotUpdateStatusWhenNoInProgressTransfersExist() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);

        doReturn(List.of()).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, never())
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(eq(List.of()),
                                                                           eq(ERROR_CODE),
                                                                           eq(ERROR_MESSAGE));
    }

    @Test
    public void whenEhrExtractStatusIsNullInterceptExceptionAndLogErrorMsg() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();
        doReturn(null).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                         any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        assertDoesNotThrow(() -> ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts());

        verify(logger).error(eq("An error occurred when closing a failed process for conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(EhrExtractException.class));
    }

    @Test
    void shouldLogWarningWithDuplicateWhenLateAcknowledgementReceivedAfter8DaysAndEhrReceivedAckErrorCodeDoNotMatch() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                                     .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                                     .taskId(generateRandomUppercaseUUID())
                                                                     .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(List.of(
                                  EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails
                                      .builder()
                                      .code(ALTERNATIVE_ERROR_CODE)
                                      .display(ERROR_MESSAGE)
                                      .build())).build())
                              .build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(logger, times(1)).warn("Received an ACK message with a conversation_id=11111 that is a duplicate");
    }

    @Test
    void shouldLogWarningWhenLateAcknowledgementReceivedAfter8DaysAndEhrReceivedAckHasErrors() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(List.of(
                                  EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails
                                      .builder()
                                      .code(ERROR_CODE)
                                      .display(ERROR_MESSAGE)
                                      .build())).build())
                              .build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(logger, times(1))
            .warn("Received an ACK message with a conversation_id: 11111, but it will be ignored.");
    }

    @Test
    void shouldNotLogWarningThatAckIsIgnoredWhenAcknowledgementReceivedIsNull() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().build())
                              .build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(logger, times(1)).warn("Received an ACK message with a conversation_id=11111 that is a duplicate");
    }

    @Test
    void doesNotUpdateEhrExtractStatusWhenEhrContinueIsPresent() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";

        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder()
                                                                      .ehrExtractCorePending(
            EhrExtractStatus.EhrExtractCorePending.builder().sentAt(Instant.now()).taskId("22222").build())
                                                                      .ehrContinue(EhrExtractStatus.EhrContinue.builder().build())
                                                                      .build());
        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        var ehrExtractStatusResult = ehrExtractStatusServiceSpy.updateEhrExtractStatusContinue(conversationId);

        assertFalse(ehrExtractStatusResult.isPresent());
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    private EhrExtractStatus addInProgressTransfers(String conversationId) {
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
            .build();

        ehrExtractStatusRepository.save(extractStatus);
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