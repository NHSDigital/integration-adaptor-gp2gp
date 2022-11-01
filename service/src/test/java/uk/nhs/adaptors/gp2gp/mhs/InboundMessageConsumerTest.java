package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

@ExtendWith(MockitoExtension.class)
public class InboundMessageConsumerTest {
    @Mock
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message mockMessage;
    @Mock
    private Session mockSession;
    @Mock
    private MDCService mdcService;
    @InjectMocks
    private InboundMessageConsumer inboundMessageConsumer;

    @Test
    @SneakyThrows
    public void When_MessageHandlerReturnsTrue_Expect_MessageIsAcknowledged() {
        when(inboundMessageHandler.handle(any())).thenReturn(true);
        inboundMessageConsumer.receive(mockMessage, mockSession);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlerReturnsFalse_Expect_MessageIsNotAcknowledged() {
        when(inboundMessageHandler.handle(any())).thenReturn(false);

        inboundMessageConsumer.receive(mockMessage, mockSession);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage, times(0)).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlerReturnsFalse_Expect_SessionToBeRolledBack() {
        when(inboundMessageHandler.handle(any())).thenReturn(false);

        inboundMessageConsumer.receive(mockMessage, mockSession);
        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockSession, times(1)).rollback();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlerThrowsException_Expect_MessageIsNotAcknowledged() {
        doThrow(new RuntimeException()).when(inboundMessageHandler).handle(mockMessage);

        inboundMessageConsumer.receive(mockMessage, mockSession);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockMessage, times(0)).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlerThrowsJMSException_Expect_SessionIsRolledBack() {
        doThrow(new JMSRuntimeException("Test")).when(inboundMessageHandler).handle(mockMessage);

        inboundMessageConsumer.receive(mockMessage, mockSession);

        verify(inboundMessageHandler).handle(mockMessage);
        verify(mockSession, times(1)).rollback();
    }

    @Test
    public void When_MessageHandlerThrowsDataAccessResourceFailure_Expect_ExceptionIsThrown() {
        doThrow(new DataAccessResourceFailureException("Test exception")).when(inboundMessageHandler).handle(mockMessage);

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
            .isThrownBy(() -> inboundMessageConsumer.receive(mockMessage, mockSession));

    }
}
