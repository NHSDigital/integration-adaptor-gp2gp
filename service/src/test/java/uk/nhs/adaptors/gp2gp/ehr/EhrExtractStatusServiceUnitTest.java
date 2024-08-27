package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.exception.NonExistingInteractionIdException;
import java.time.Duration;

import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrExtractStatusServiceUnitTest {

    public static final int NINE_DAYS = 9;
    public static final int EIGHT_DAYS = 8;
    public static final String ERROR_CODE = "04";
    public static final String ERROR_MESSAGE = "The acknowledgement has been received after 8 days";
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
    void ehrExtractStatusIsNotUpdatedWhenDelayBewteenAckUpdatesIsMoreThan8Days() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant nineDaysAgo = currentInstant.minus(Duration.ofDays(NINE_DAYS));
        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder().updatedAt(nineDaysAgo).build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(false).when(ehrExtractStatusServiceSpy).hasFinalAckBeenReceived(conversationId);
        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        doReturn(ehrExtractStatus.get())
            .when(mongoTemplate).findAndModify(any(Query.class), any(Update.class), any(), any(Class.class));

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(ehrExtractStatusServiceSpy).updateEhrExtractStatusError(conversationId,
                                                                       ERROR_CODE,
                                                                       ERROR_MESSAGE,
                                                                       ehrExtractStatusServiceSpy.getClass().getSimpleName());
    }

    @Test
    void ehrExtractStatusThrowsExceptionWhenConversationIdNotFound() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant nineDaysAgo = currentInstant.minus(Duration.ofDays(NINE_DAYS));
        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder().updatedAt(nineDaysAgo).build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(false).when(ehrExtractStatusServiceSpy).hasFinalAckBeenReceived(conversationId);
        doReturn(Optional.empty()).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);

        Exception exception = assertThrows(NonExistingInteractionIdException.class, () ->
            ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack));

        assertEquals("Received a ACK message that is not recognised with conversation_id: " + conversationId,
                     exception.getMessage());

        verify(ehrExtractStatusServiceSpy, never()).updateEhrExtractStatusError(conversationId,
                                                                       ERROR_CODE,
                                                                       ERROR_MESSAGE,
                                                                       ehrExtractStatusServiceSpy.getClass().getSimpleName());
    }

    @Test
    void ehrExtractStatusIsUpdatedWhenDelayBewteenAckUpdatesIsLessThan8Days() {
        EhrExtractStatusService ehrExtractStatusServiceSpy = spy(ehrExtractStatusService);
        String conversationId = "11111";
        Instant currentInstant = Instant.now();
        Instant eightDaysAgo = currentInstant.minus(Duration.ofDays(EIGHT_DAYS));
        Optional<EhrExtractStatus> ehrExtractStatus = Optional.of(EhrExtractStatus.builder().updatedAt(eightDaysAgo).build());

        doReturn(true).when(ehrExtractStatusServiceSpy).isEhrStatusWaitingForFinalAck(conversationId);
        doReturn(false).when(ehrExtractStatusServiceSpy).hasFinalAckBeenReceived(conversationId);
        doReturn(ehrExtractStatus).when(ehrExtractStatusRepository).findByConversationId(conversationId);
        when(ack.getErrors()).thenReturn(null);
        when(ack.getReceived()).thenReturn(currentInstant);
        doReturn(ehrExtractStatus.get())
            .when(mongoTemplate).findAndModify(any(Query.class), any(Update.class), any(), any(Class.class));

        ehrExtractStatusServiceSpy.updateEhrExtractStatusAck(conversationId, ack);

        verify(ehrExtractStatusServiceSpy, never()).updateEhrExtractStatusError(conversationId,
                                                                                ERROR_CODE,
                                                                                ERROR_MESSAGE,
                                                                       ehrExtractStatusServiceSpy.getClass().getSimpleName());
    }

}