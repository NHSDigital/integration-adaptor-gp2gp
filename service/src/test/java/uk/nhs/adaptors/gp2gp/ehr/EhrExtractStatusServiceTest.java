package uk.nhs.adaptors.gp2gp.ehr;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnrecognisedInteractionIdException;
import java.time.Duration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrExtractStatusServiceTest {

    public static final int NINE_DAYS = 9;
    public static final int EIGHT_DAYS = 8;
    public static final String ERROR_CODE = "99";
    public static final String ALTERNATIVE_ERROR_CODE = "26";
    public static final String ERROR_MESSAGE = "The acknowledgement has been received after 8 days";
    private static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    public static final String ACK_TYPE = "AA";
    public static final int TWENTY_DAYS = 20;

    private ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    private ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
    private ArgumentCaptor<Class<EhrExtractStatus>> classCaptor = ArgumentCaptor.forClass(Class.class);

    @Mock
    private Logger logger;

    @Mock
    private MongoTemplate mongoTemplate;

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
    void shouldLogWarningWhenLateAcknowledgementReceivedAfter8DaysAndEhrReceivedAckHasErrors() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
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
            .warn("Received an ACK message with a conversation_id: {}, but it will be ignored", conversationId);
    }

    @Test
    void shouldNotLogWarningThatAckIsIgnoredWhenAcknowledgementReceivedAfter8Days() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
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

        verify(logger, never())
            .warn("Received an ACK message with a conversation_id: {}, but it will be ignored", conversationId);
        verify(logger, times(1))
            .warn("Received an ACK message with a conversation_id: {} that is a duplicate", conversationId);
    }

    @Test
    void shouldNotLogWarningThatAckIsIgnoredWhenAcknowledgementReceivedAfterWithinAndThereAreNoErrors() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        ReflectionTestUtils.setField(ehrExtractStatusServiceSpy, "ehrExtractSentDaysLimit", EIGHT_DAYS);
        String conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(EIGHT_DAYS)))
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

        verify(logger, never()).warn("Received an ACK message with a conversation_id: {}, but it will be ignored", conversationId);
        verify(logger, times(1))
            .warn("Received an ACK message with a conversation_id: {} that is a duplicate", conversationId);
    }

    @Test
    void updateEhrExtractStatusWhenEhrExtractCorePendingIsNull() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder().ehrExtractCorePending(null).build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(false).when(ehrExtractStatusServiceSpy).hasFinalAckBeenReceived(conversationId);
        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        doReturn(ehrExtractStatus.get())
            .when(mongoTemplate).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), any(Class.class));
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(logger, never()).warn("Received an ACK message with a conversation_id={} exceeded 8 days", conversationId);
        verify(logger, times(1))
                            .info("Database successfully updated with EHRAcknowledgement, conversation_id: {}", conversationId);
    }

    @Test
    void ehrExtractStatusThrowsExceptionWhenConversationIdNotFound() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(Optional.empty()).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);

        Exception exception = assertThrows(UnrecognisedInteractionIdException.class, () ->
            ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack));

        assertEquals("Received an unrecognized ACK message with conversation_id: " + conversationId,
                     exception.getMessage());

        verify(ehrExtractStatusServiceSpy, never()).updateEhrExtractStatusError(conversationId,
                                                                                ERROR_CODE,
                                                                                ERROR_MESSAGE,
                                                                                ehrExtractStatusServiceSpy.getClass().getSimpleName());
    }

    @Test
    void updateEhrExtractWhenDelayBewteenAckUpdatesIsLessThan8Days() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));
        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder().updatedAt(eightDaysAgo).build());

        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(ehrExtractStatusServiceSpy, never()).updateEhrExtractStatusError(conversationId,
                                                                                ERROR_CODE,
                                                                                ERROR_MESSAGE,
                                                                                ehrExtractStatusServiceSpy.getClass().getSimpleName());
    }

    @Test
    void doesNotUpdateEhrExtractStatusWhenEhrContinueIsPresent() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();

        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder()
                                                                      .ehrExtractCorePending(
            EhrExtractStatus.EhrExtractCorePending.builder().sentAt(Instant.now()).taskId("22222").build())
                                                                      .ehrContinue(EhrExtractStatus.EhrContinue.builder().build())
                                                                      .build());
        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        var ehrExtractStatusResult = ehrExtractStatusServiceSpy.updateEhrExtractStatusContinue(conversationId);

        assertFalse(ehrExtractStatusResult.isPresent());
    }

    @Test
    void expectFalseWhenEhrExtractStatusWithEhrReceivedAckWithErrorsDoesNotMatchExpectedErrorType() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
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

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        boolean result = ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId);

        assertFalse(result);
    }

    @Test
    void expectFalseWhenEhrExtractStatusWithEhrReceivedAckWithErrorsIsNull() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(null).build())
                              .build());

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        assertFalse(ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId));
    }

    @Test
    void expectFalseWhenEhrExtractStatusWithEhrReceivedAckWithEmptyErrorsList() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(List.of()).build())
                              .build());

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        assertFalse(ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId));
    }

    @Test
    void expectFalseWhenhrReceivedAckIsNull() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(NINE_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(null)
                              .build());

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        assertFalse(ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId));
    }

    @Test
    void expectNoExceptionWhenEhrExtractStatusWithEhrReceivedAckWithErrorsIncludesNullValue() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
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
                                      .code(null)
                                      .display(ERROR_MESSAGE)
                                      .build())).build())
                              .build());

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        assertDoesNotThrow(() -> ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId));
    }

    @Test
    void expectTrueWhenEhrExtractStatusWithEhrReceivedAckWithErrorsThatMatchExpectedErrorType() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var conversationId = generateRandomUppercaseUUID();
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

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        boolean result = ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithUnexpectedConditionErrors(conversationId);

        assertTrue(result);
    }

    @Test
    void shouldUpdateStatusWithErrorWhenEhrExtractAckTimeoutOccurs() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();
        doReturn(ehrExtractStatusUpdated).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                         any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, times(1))
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(any(Stream.class),
                                                                           eq(ERROR_CODE),
                                                                           eq(ERROR_MESSAGE));
        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                               + "updated in the database with error information conversation_id: {}", inProgressConversationId);
    }

    @Test
    void shouldUpdateStatusWithErrorAndSpecificErrorCodeAndMessage() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(ehrExtractStatusUpdated).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                                            any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(Stream.of(ehrExtractStatus),
                                                                                                 ERROR_CODE,
                                                                                                 ERROR_MESSAGE);

        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                            + "updated in the database with error information conversation_id: {}", inProgressConversationId);
        verify(mongoTemplate, times(1)).findAndModify(queryCaptor.capture(),
                                                                            updateCaptor.capture(),
                                                                            any(FindAndModifyOptions.class),
                                                                            classCaptor.capture());

        assertEquals(ERROR_CODE, ((EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails) ((Document) updateCaptor.getValue()
                                                                                .getUpdateObject()
                                                                                .get("$addToSet"))
                                                                                .get("ehrReceivedAcknowledgement.errors")).getCode());
        assertEquals(ERROR_MESSAGE, ((EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails) ((Document) updateCaptor.getValue()
                                                                                .getUpdateObject()
                                                                                .get("$addToSet"))
                                                                                .get("ehrReceivedAcknowledgement.errors")).getDisplay());
    }

    @Test
    void shouldNotUpdateStatusWhenEhrReceivedAcknowledgementIsNotNull() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().build());

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, never())
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(Stream.of(ehrExtractStatus), ERROR_CODE, ERROR_MESSAGE);
    }

    @Test
    void shouldNotUpdateStatusWhenNoInProgressTransfersExist() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);

        doReturn(List.of()).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, never())
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(Stream.of(), ERROR_CODE, ERROR_MESSAGE);
    }

    @Test
    void shouldNotUpdateStatusWhenInProgressTransfersWithNullEhrExtractCorePendingExist() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        ehrExtractStatus.setEhrExtractCorePending(null);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, never())
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(Stream.of(), ERROR_CODE, ERROR_MESSAGE);
    }

    @Test
    void shouldNotDoAnythingIfThereAreNoEhrExtractStatusThatExceeded8Days() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        ReflectionTestUtils.setField(ehrExtractStatusServiceSpy, "ehrExtractSentDaysLimit", EIGHT_DAYS);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        ehrExtractStatus.setEhrExtractCorePending(
            EhrExtractStatus.EhrExtractCorePending
                .builder()
                .sentAt(FIVE_DAYS_AGO)
                .build());

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();

        ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts();

        verify(ehrExtractStatusServiceSpy, never())
            .updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(Stream.of(), ERROR_CODE, ERROR_MESSAGE);
    }

    @Test
    void updateEhrExtractStatusListWithEhrReceivedAcknowledgementError() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(null).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                                    any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        var exception = assertThrows(EhrExtractException.class,
                                     () -> ehrExtractStatusServiceSpy.updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(
                                         Stream.of(ehrExtractStatus), ERROR_CODE, ERROR_MESSAGE));

        assertEquals("Couldn't update EHR received acknowledgement with error information because EHR status doesn't exist, "
                     + "conversation_id: " + inProgressConversationId,
                     exception.getMessage());
        verify(logger).error(eq("An error occurred when updating EHR Extract with Ack erorrs, EHR Extract Status conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(EhrExtractException.class));
    }

    @Test
    void shouldCatchExceptionIfUnexpectedConditionAriseWhileUpdatingEhrExtractStatusListWithEhrReceivedAcknowledgementError() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doThrow(new NullPointerException())
            .when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        assertThrows(Exception.class,
                         () -> ehrExtractStatusServiceSpy.updateEhrExtractStatusListWithEhrReceivedAcknowledgementError(
                             Stream.of(ehrExtractStatus), ERROR_CODE, ERROR_MESSAGE));

        verify(logger).error(eq("An unexpected error occurred for conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(NullPointerException.class));
    }

    @Test
    void whenEhrExtractStatusIsNullInterceptExceptionAndLogErrorMsg() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractStatusServiceSpy).findInProgressTransfers();
        doReturn(null).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                         any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        var exception = assertThrows(EhrExtractException.class, () -> ehrExtractStatusServiceSpy.checkForEhrExtractAckTimeouts());

        assertEquals("Couldn't update EHR received acknowledgement with error information because EHR status doesn't exist, "
                     + "conversation_id: " + inProgressConversationId,
                     exception.getMessage());
        verify(logger).error(eq("An error occurred when updating EHR Extract with Ack erorrs, EHR Extract Status conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(EhrExtractException.class));
    }

    @Test
    void shouldLogWarningWithDuplicateWhenLateAcknowledgementReceivedAfter8DaysAndEhrReceivedAckErrorCodeDoNotMatch() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = generateRandomUppercaseUUID();
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

        verify(logger, times(1))
            .warn("Received an ACK message with a conversation_id: {} that is a duplicate", conversationId);
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