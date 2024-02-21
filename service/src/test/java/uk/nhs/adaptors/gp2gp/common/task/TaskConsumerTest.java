package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.Message;
import jakarta.jms.Session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

@ExtendWith(MockitoExtension.class)
public class TaskConsumerTest {

    @Mock
    private TaskHandler taskHandler;

    @InjectMocks
    private TaskConsumer taskConsumer;

    @Mock
    private MDCService mdcService;

    @Mock
    private Message message;

    @Mock
    private Session session;

    @Test
    @SneakyThrows
    public void When_TaskHandlerReturnsTrue_Expect_MessageAcknowledged() {
        when(taskHandler.handle(any())).thenReturn(true);

        taskConsumer.receive(message, session);

        verify(taskHandler).handle(message);
        verify(message).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerReturnsFalse_Expect_MessageNotAcknowledged() {
        when(taskHandler.handle(any())).thenReturn(false);

        taskConsumer.receive(message, session);

        verify(taskHandler).handle(message);
        verify(message, times(0)).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerReturnsFalse_Expect_SessionRolledBack() {
        when(taskHandler.handle(any())).thenReturn(false);

        taskConsumer.receive(message, session);

        verify(taskHandler).handle(message);
        verify(session, times(1)).rollback();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerThrowsException_Expect_MessageNotAcknowledged() {
        doThrow(RuntimeException.class).when(taskHandler).handle(message);

        taskConsumer.receive(message, session);

        verify(taskHandler).handle(message);
        verify(message, times(0)).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerThrowsException_Expect_SessionRolledBack() {
        doThrow(RuntimeException.class).when(taskHandler).handle(message);

        taskConsumer.receive(message, session);

        verify(taskHandler).handle(message);
        verify(session, times(1)).rollback();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerThrowsDataResourceAccessFailureException_Expect_ExceptionIsThrown() {
        doThrow(DataAccessResourceFailureException.class).when(taskHandler).handle(message);

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
            .isThrownBy(() -> taskConsumer.receive(message, session));

        verify(session, times(0)).rollback();
        verify(message, times(0)).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerThrowsMhsConnectionException_Expect_ExceptionIsThrown() {
        doThrow(MhsConnectionException.class).when(taskHandler).handle(message);

        assertThatExceptionOfType(MhsConnectionException.class)
            .isThrownBy(() -> taskConsumer.receive(message, session));

        verify(session, times(0)).rollback();
        verify(message, times(0)).acknowledge();
    }
}
