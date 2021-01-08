package uk.nhs.adaptors.gp2gp.common.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.Session;
import javax.jms.TextMessage;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.common.task.TaskHandler.TASK_TYPE_HEADER_NAME;

@ExtendWith(MockitoExtension.class)
public class TaskDispatcherTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaskDispatcher taskDispatcher;

    @Mock
    private Session session;

    @Mock
    private TextMessage textMessage;

    @Test
    @SneakyThrows
    public void When_CreateTask_Expect_TaskPublishedToQueue() {
        TaskDefinition taskDefinition = mock(TaskDefinition.class);
        when(taskDefinition.getTaskType()).thenReturn(TaskType.GET_GPC_DOCUMENT);
        when(objectMapper.writeValueAsString(taskDefinition)).thenReturn("payload");

        taskDispatcher.createTask(taskDefinition);

        var messageCreatorArgumentCaptor = ArgumentCaptor.forClass(MessageCreator.class);
        String queueName = null;
        verify(jmsTemplate).send(eq(queueName), messageCreatorArgumentCaptor.capture());

        when(session.createTextMessage("payload")).thenReturn(textMessage);

        messageCreatorArgumentCaptor.getValue().createMessage(session);
        verify(textMessage).setStringProperty(TASK_TYPE_HEADER_NAME, TaskType.GET_GPC_DOCUMENT.getTaskTypeHeaderValue());
    }

    @Test
    @SneakyThrows
    public void When_TaskNotParsed_Expect_ExceptionThrown() {
        TaskDefinition taskDefinition = mock(TaskDefinition.class);
        doThrow(JsonProcessingException.class)
            .when(objectMapper).writeValueAsString(taskDefinition);

        assertThatExceptionOfType(TaskHandlerException.class)
            .isThrownBy(() -> taskDispatcher.createTask(taskDefinition))
            .withMessageContaining("Unable to serialise task definition to JSON");

        verifyNoInteractions(jmsTemplate);
    }

}
