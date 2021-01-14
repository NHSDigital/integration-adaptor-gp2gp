package uk.nhs.adaptors.gp2gp.common.task;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.common.task.TaskHandler.TASK_TYPE_HEADER_NAME;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
public class TaskHandlerTest {

    @Mock
    private TaskDefinitionFactory taskDefinitionFactory;

    @Mock
    private TaskExecutorFactory taskExecutorFactory;

    @Mock
    private MDCService mdcService;

    @InjectMocks
    private TaskHandler taskHandler;

    @Mock
    private Message message;

    @Mock
    private TaskDefinition taskDefinition;

    @Mock
    private TaskExecutor taskExecutor;

    @Test
    @SneakyThrows
    public void When_TaskHandled_Expect_TaskExecuted() {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn("taskType");
        when(message.getBody(String.class)).thenReturn("body");
        when(taskDefinitionFactory.getTaskDefinition("taskType", "body")).thenReturn(taskDefinition);
        when(taskExecutorFactory.getTaskExecutor(taskDefinition.getClass())).thenReturn(taskExecutor);

        taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
    }

    @Test
    @SneakyThrows
    public void When_JmsError_Expect_ExceptionThrown() {
        doThrow(JMSException.class).when(message).getStringProperty(TASK_TYPE_HEADER_NAME);

        assertThatExceptionOfType(TaskHandlerException.class)
            .isThrownBy(() -> taskHandler.handle(message))
            .withMessageContaining("Unable to read task definition from JMS message");
    }

}
