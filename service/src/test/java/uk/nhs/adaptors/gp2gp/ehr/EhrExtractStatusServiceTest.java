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
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnrecognisedInteractionIdException;

import java.lang.reflect.Field;
import java.time.Duration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    public static final int NINE_DAYS = 9;
    public static final int EIGHT_DAYS = 8;
    public static final String ALTERNATIVE_ERROR_CODE = "26";
    public static final String ERROR_CODE = "99";
    public static final String ERROR_MESSAGE = "No acknowledgement has been received within ACK timeout limit";
    public static final int EHR_EXTRACT_SENT_DAYS_LIMIT = 8;

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
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        ehrExtractStatusService = new EhrExtractStatusService(mongoTemplate, ehrExtractStatusRepository, timestampService);

        Field field = EhrExtractStatusService.class.getDeclaredField("ehrExtractSentDaysLimit");
        field.setAccessible(true);
        field.set(ehrExtractStatusService, EHR_EXTRACT_SENT_DAYS_LIMIT);
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
            .warn("Received an ACK message with conversation_id: {}, "
                  + "but it is being ignored because the EhrExtract has already been marked as failed "
                  + "from not receiving an acknowledgement from the requester in time.",
                  conversationId);
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
    void shouldUpdateStatusWithErrorAndSpecificErrorCodeAndMessage() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(ehrExtractStatusUpdated).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                                            any(FindAndModifyOptions.class), any());
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                                                 ERROR_CODE,
                                                                                 ERROR_MESSAGE);

        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                            + "updated in the database with error information conversation_id: {}", inProgressConversationId);
        verify(mongoTemplate, times(1)).findAndModify(queryCaptor.capture(),
                                                      updateCaptor.capture(),
                                                      any(FindAndModifyOptions.class),
                                                      classCaptor.capture());

        assertEquals(ERROR_CODE,
                     (((EhrExtractStatus.EhrReceivedAcknowledgement) ((Document)
                              updateCaptor.getValue().getUpdateObject()
                                  .get("$set"))
                                  .get("ehrReceivedAcknowledgement"))
                                  .getErrors().get(0))
                                  .getCode());
        assertEquals(ERROR_MESSAGE,
                     (((EhrExtractStatus.EhrReceivedAcknowledgement) ((Document)
                              updateCaptor.getValue().getUpdateObject()
                                  .get("$set"))
                                  .get("ehrReceivedAcknowledgement"))
                                  .getErrors().get(0))
                                  .getDisplay());
    }

    @Test
    void shouldNotLogWarningThatAckIsIgnoredWhenAcknowledgementReceivedAfterWithinAndThereAreNoErrors() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
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

        verify(logger, never())
            .warn("Received an ACK message with a conversation_id: {}, but it will be ignored", conversationId);
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
    void expectTrueWhenEhrExtractStatusWithEhrReceivedAckWithErrorsAndExceededTimeoutLimit() {

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

        boolean result = ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithErrors(conversationId);

        assertTrue(result);
    }

    @Test
    void expectFalseWhenEhrExtractStatusWithEhrReceivedAckWithNoErrors() {

        var conversationId = generateRandomUppercaseUUID();
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));

        Optional<EhrExtractStatus> ehrExtractStatusWithEhrReceivedAckWithErrors
            = Optional.of(EhrExtractStatus.builder()
                              .updatedAt(eightDaysAgo)
                              .ehrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder()
                                                         .sentAt(currentInstant.minus(Duration.ofDays(EIGHT_DAYS)))
                                                         .taskId(generateRandomUppercaseUUID())
                                                         .build())
                              .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(null).build())
                              .build());

        doReturn(ehrExtractStatusWithEhrReceivedAckWithErrors).when(ehrExtractStatusRepository).findByConversationId(conversationId);

        boolean result = ehrExtractStatusService.hasEhrStatusReceivedAckWithErrors(conversationId);

        assertFalse(result);
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

        assertFalse(ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithErrors(conversationId));
    }

    @Test
    void shouldUpdateStatusWithErrorWhenEhrExtractAckTimeoutOccurs() {

        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);

        var inProgressConversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(new Update()).when(ehrExtractStatusServiceSpy).createUpdateWithUpdatedAt();
        doReturn(new Query()).when(ehrExtractStatusServiceSpy).createQueryForConversationId(inProgressConversationId);
        doReturn(new FindAndModifyOptions()).when(ehrExtractStatusServiceSpy).getReturningUpdatedRecordOption();
        doReturn(ehrExtractStatusUpdated).when(mongoTemplate)
            .findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), eq(EhrExtractStatus.class));
        when(ehrExtractStatusServiceSpy.logger()).thenReturn(logger);

        ehrExtractStatusServiceSpy.updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                                                 ERROR_CODE,
                                                                                 ERROR_MESSAGE);

        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                            + "updated in the database with error information conversation_id: {}", inProgressConversationId);
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

        assertFalse(ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithErrors(conversationId));
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

        assertDoesNotThrow(() -> ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithErrors(conversationId));
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

        boolean result = ehrExtractStatusServiceSpy.hasEhrStatusReceivedAckWithErrors(conversationId);

        assertTrue(result);
    }

    @Test
    void shouldLogWarningWithMsgIgnoredWhenLateAcknowledgementReceivedAfter8DaysAndEhrReceivedAckErrorCodeDoNotMatch() {

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
                                    .warn("Received an ACK message with conversation_id: {}, "
                                          + "but it is being ignored because the EhrExtract has already been marked as failed "
                                          + "from not receiving an acknowledgement from the requester in time.",
                                          conversationId);
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

}