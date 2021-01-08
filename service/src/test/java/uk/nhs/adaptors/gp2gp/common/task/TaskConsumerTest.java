package uk.nhs.adaptors.gp2gp.common.task;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jms.Message;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TaskConsumerTest {

    @Mock
    private TaskHandler taskHandler;

    @InjectMocks
    private TaskConsumer taskConsumer;

    @Mock
    private Message message;

    @Test
    @SneakyThrows
    public void When_TaskHandled_Expect_MessageAcknowledged() {
        taskConsumer.receive(message);

        verify(taskHandler).handle(message);
        verify(message).acknowledge();
    }

    @Test
    @SneakyThrows
    public void When_TaskHandlerError_Expect_MessageNotAcknowledged() {
        doThrow(RuntimeException.class).when(taskHandler).handle(message);

        taskConsumer.receive(message);

        verify(taskHandler).handle(message);
        verify(message, times(0)).acknowledge();
    }

}
