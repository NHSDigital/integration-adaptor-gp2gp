package uk.nhs.adaptors.gp2gp.ehr.scheduling;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
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
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class EhrExtractTimeoutSchedulerTest {

    public static final int NINE_DAYS = 9;
    public static final int EIGHT_DAYS = 8;

    private static final Instant NOW = Instant.now();
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    public static final String ACK_TYPE = "AA";
    public static final int TWENTY_DAYS = 20;
    private static final String UNEXPECTED_CONDITION_ERROR_CODE = "99";
    private static final String UNEXPECTED_CONDITION_ERROR_MESSAGE = format("No acknowledgement has been received within %s days", 8);

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
    private EhrExtractStatusService ehrExtractStatusService;

    @InjectMocks
    private EhrExtractTimeoutScheduler ehrExtractTimeoutScheduler;

    @BeforeEach
    void setUp() {
        ehrExtractTimeoutScheduler = new EhrExtractTimeoutScheduler(mongoTemplate, ehrExtractStatusService);
    }

    @AfterEach
    void tearDown() {
        mongoTemplate = null;
        ehrExtractTimeoutScheduler = null;
        ehrExtractStatusService = null;
    }

    @Test
    void shouldReturnCorrectLoggerInstance() {
        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);

        assertNotNull(ehrExtractTimeoutSchedulerSpy.logger());
        assertTrue(logger instanceof Logger);
    }

    @Test
    void shouldReturnInProgressTransfersWithCorrectCriteria() {

        List<EhrExtractStatus> expectedResult = List.of(new EhrExtractStatus());
        when(mongoTemplate.find(any(Query.class), eq(EhrExtractStatus.class))).thenReturn(expectedResult);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        final int expectedConditions = 3;

        List<EhrExtractStatus> result = ehrExtractTimeoutScheduler.findInProgressTransfers();

        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(EhrExtractStatus.class));

        assertEquals(expectedResult, result);

        Query capturedQuery = queryCaptor.getValue();

        assertTrue(capturedQuery.getQueryObject().containsKey("$nor"), "Query should contain a NOR operator");
        assertEquals(expectedConditions, ((List<?>) capturedQuery.getQueryObject().get("$nor")).size(),
                                    "NOR operator should have 3 conditions");
        assertEquals(true, ((Document) ((List<Document>) ((List<Document>) capturedQuery
                                                                                    .getQueryObject()
                                                                                    .get("$nor"))
                                                                                    .get(2)
                                                                                    .get("$and"))
                                                                                    .get(0)
                                                                                    .get("ehrReceivedAcknowledgement.errors"))
                                                                                    .get("$exists"));
    }

    @Test
    void shouldUpdateStatusWithErrorWhenEhrExtractAckTimeoutOccurs() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(true).when(ehrExtractStatusService).hasLastUpdateExceededEightDays(any(EhrExtractStatus.class),
                                                                                    any(Instant.class));
        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();

        doReturn(new Update()).when(ehrExtractStatusService).createUpdateWithUpdatedAt();
        doReturn(new Query()).when(ehrExtractStatusService).createQueryForConversationId(inProgressConversationId);
        doReturn(new FindAndModifyOptions()).when(ehrExtractStatusService).getReturningUpdatedRecordOption();
        doReturn(ehrExtractStatusUpdated)
            .when(mongoTemplate)
            .findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), eq(EhrExtractStatus.class));
        when(ehrExtractTimeoutSchedulerSpy.logger()).thenReturn(logger);

        ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts();

        verify(ehrExtractTimeoutSchedulerSpy, times(1))
            .updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                           UNEXPECTED_CONDITION_ERROR_CODE,
                                                           UNEXPECTED_CONDITION_ERROR_MESSAGE);
        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                            + "updated in the database with error information conversation_id: {}", inProgressConversationId);
    }

    @Test
    void shouldUpdateStatusWithErrorAndSpecificErrorCodeAndMessage() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatusUpdated = EhrExtractStatus.builder().build();

        doReturn(new Update()).when(ehrExtractStatusService).createUpdateWithUpdatedAt();
        doReturn(new Query()).when(ehrExtractStatusService).createQueryForConversationId(inProgressConversationId);
        doReturn(new FindAndModifyOptions()).when(ehrExtractStatusService).getReturningUpdatedRecordOption();
        doReturn(ehrExtractStatusUpdated).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                                            any(FindAndModifyOptions.class), any());

        when(ehrExtractTimeoutSchedulerSpy.logger()).thenReturn(logger);

        ehrExtractTimeoutSchedulerSpy.updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                                                    UNEXPECTED_CONDITION_ERROR_CODE,
                                                                                    UNEXPECTED_CONDITION_ERROR_MESSAGE);

        verify(logger).info("EHR status (EHR received acknowledgement) record successfully "
                            + "updated in the database with error information conversation_id: {}", inProgressConversationId);
        verify(mongoTemplate, times(1)).findAndModify(queryCaptor.capture(),
                                                      updateCaptor.capture(),
                                                      any(FindAndModifyOptions.class),
                                                      classCaptor.capture());

        assertEquals(UNEXPECTED_CONDITION_ERROR_CODE,
                     ((EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails) ((Document) updateCaptor.getValue()
                         .getUpdateObject()
                         .get("$addToSet"))
                         .get("ehrReceivedAcknowledgement.errors")).getCode());
        assertEquals(UNEXPECTED_CONDITION_ERROR_MESSAGE,
                     ((EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails) ((Document) updateCaptor.getValue()
                         .getUpdateObject()
                         .get("$addToSet"))
                         .get("ehrReceivedAcknowledgement.errors")).getDisplay());
    }

    @Test
    void shouldNotUpdateStatusWhenEhrReceivedAcknowledgementIsNotNull() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().build());

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();

        ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts();

        verify(logger, never()).info("Scheduler has started processing EhrExtract list with Ack timeouts");
        verify(ehrExtractTimeoutSchedulerSpy, never())
            .updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                           UNEXPECTED_CONDITION_ERROR_CODE,
                                                           UNEXPECTED_CONDITION_ERROR_MESSAGE);
    }

    @Test
    void shouldNotUpdateStatusWhenNoInProgressTransfersExist() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);

        doReturn(List.of()).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();

        ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts();

        verify(ehrExtractTimeoutSchedulerSpy, never())
            .updateEhrExtractStatusWithEhrReceivedAckError(null,
                                                           UNEXPECTED_CONDITION_ERROR_CODE,
                                                           UNEXPECTED_CONDITION_ERROR_MESSAGE);
    }

    @Test
    void shouldNotUpdateStatusWhenInProgressTransfersWithNullEhrExtractCorePendingExist() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        ehrExtractStatus.setEhrExtractCorePending(null);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();

        ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts();

        verify(ehrExtractTimeoutSchedulerSpy, never())
            .updateEhrExtractStatusWithEhrReceivedAckError(ehrExtractStatus.getConversationId(),
                                                           UNEXPECTED_CONDITION_ERROR_CODE,
                                                           UNEXPECTED_CONDITION_ERROR_MESSAGE);
    }

    @Test
    void updateEhrExtractStatusListWithEhrReceivedAcknowledgementError() {
        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);

        var inProgressConversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(new Update()).when(ehrExtractStatusService).createUpdateWithUpdatedAt();
        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();
        doReturn(true).when(ehrExtractStatusService).hasLastUpdateExceededEightDays(any(EhrExtractStatus.class),
                                                                                    any(Instant.class));

        when(ehrExtractTimeoutSchedulerSpy.logger()).thenReturn(logger);

        var exception = assertThrows(EhrExtractException.class, () -> ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts());

        verify(logger).info("Scheduler has started processing EhrExtract list with Ack timeouts");
        assertEquals("Couldn't update EHR received acknowledgement with error information because EHR status doesn't exist, "
                     + "conversation_id: " + inProgressConversationId, exception.getMessage());
        verify(logger).error(eq("An error occurred when updating EHR Extract with Ack erorrs, EHR Extract Status conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(EhrExtractException.class));
    }

    @Test
    void shouldCatchExceptionIfUnexpectedConditionAriseWhileUpdatingEhrExtractStatusListWithEhrReceivedAcknowledgementError() {
        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();
        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);
        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();

        doReturn(true).when(ehrExtractStatusService).hasLastUpdateExceededEightDays(any(), any());
        doThrow(new NullPointerException())
            .when(ehrExtractTimeoutSchedulerSpy).updateEhrExtractStatusWithEhrReceivedAckError(inProgressConversationId,
                                                                                               UNEXPECTED_CONDITION_ERROR_CODE,
                                                                                               UNEXPECTED_CONDITION_ERROR_MESSAGE);
        when(ehrExtractTimeoutSchedulerSpy.logger()).thenReturn(logger);

        assertThrows(Exception.class, () -> ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts());

        verify(logger).error(eq("An unexpected error occurred for conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(NullPointerException.class));
    }

    @Test
    void whenEhrExtractStatusIsNullInterceptExceptionAndLogErrorMsg() {

        EhrExtractTimeoutScheduler ehrExtractTimeoutSchedulerSpy = spy(ehrExtractTimeoutScheduler);
        var inProgressConversationId = generateRandomUppercaseUUID();

        EhrExtractStatus ehrExtractStatus = addInProgressTransfers(inProgressConversationId);

        doReturn(List.of(ehrExtractStatus)).when(ehrExtractTimeoutSchedulerSpy).findInProgressTransfers();
        doReturn(true).when(ehrExtractStatusService).hasLastUpdateExceededEightDays(any(), any());
        doReturn(new Update()).when(ehrExtractStatusService).createUpdateWithUpdatedAt();
        doReturn(new Query()).when(ehrExtractStatusService).createQueryForConversationId(inProgressConversationId);
        doReturn(new FindAndModifyOptions()).when(ehrExtractStatusService).getReturningUpdatedRecordOption();
        doReturn(null).when(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                                                         any(FindAndModifyOptions.class), any());
        when(ehrExtractTimeoutSchedulerSpy.logger()).thenReturn(logger);

        var exception = assertThrows(EhrExtractException.class, () -> ehrExtractTimeoutSchedulerSpy.processEhrExtractAckTimeouts());

        assertEquals("Couldn't update EHR received acknowledgement with error information because EHR status doesn't exist, "
                     + "conversation_id: " + inProgressConversationId, exception.getMessage());
        verify(logger).error(eq("An error occurred when updating EHR Extract with Ack erorrs, EHR Extract Status conversation_id: {}"),
                             eq(inProgressConversationId),
                             any(EhrExtractException.class));
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
                                       .sentAt(Instant.now().minus(Duration.ofDays(TWENTY_DAYS)))
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