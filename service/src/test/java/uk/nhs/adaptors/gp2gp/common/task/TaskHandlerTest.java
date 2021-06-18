package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.task.TaskHandler.TASK_TYPE_HEADER_NAME;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
public class TaskHandlerTest {

    private static final String NACK_ERROR_CODE = "18";

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

    @Mock
    private ProcessFailureHandlingService processFailureHandlingService;

    @Test
    @SneakyThrows
    public void When_TaskHandled_Expect_TaskExecuted() {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn("taskType");
        when(message.getBody(String.class)).thenReturn("body");
        when(taskDefinitionFactory.getTaskDefinition("taskType", "body")).thenReturn(taskDefinition);
        when(taskExecutorFactory.getTaskExecutor(taskDefinition.getClass())).thenReturn(taskExecutor);

        var result = taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
        assertThat(result).isTrue();
    }

    @Test
    @SneakyThrows
    public void When_JmsError_Expect_ExceptionThrown() {
        doThrow(JMSException.class).when(message).getStringProperty(TASK_TYPE_HEADER_NAME);

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_NackTaskFails_Expect_ProcessNotToBeFailed() {
        setupValidMessage("conversationId1", TaskType.SEND_NEGATIVE_ACKNOWLEDGEMENT);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
        verify(taskExecutor).execute(taskDefinition);
        verify(processFailureHandlingService, never()).failProcess(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    public void When_NonNackTaskFails_Expect_ProcessToBeFailed() {
        String conversationId = "conversationId1";
        TaskType taskType = TaskType.SEND_EHR_CONTINUE;
        setupValidMessage(conversationId, taskType);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
        verify(processFailureHandlingService).failProcess(
            conversationId,
            NACK_ERROR_CODE,
            "An error occurred when executing a task",
            taskType.name()
        );
    }

    @Test
    @SneakyThrows
    public void When_NonNackTaskFails_Expect_ResultFromFailureHandlerToBeReturned() {
        setupValidMessage("conversationId1", TaskType.SEND_EHR_CONTINUE);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertThat(taskHandler.handle(message)).isTrue();
        assertThat(taskHandler.handle(message)).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_FailureHandlerThrowsException_Expect_ExceptionToBeRethrown() {
        setupValidMessage("conversationId1", TaskType.SEND_EHR_CONTINUE);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        doThrow(new RuntimeException("task executor exception")).when(taskExecutor).execute(any());

        var failureHandlingException = new RuntimeException("failure handler exception");
        doThrow(failureHandlingException).when(processFailureHandlingService).failProcess(any(), any(), any(), any());

        assertThatThrownBy(
            () -> taskHandler.handle(message)
        ).isSameAs(failureHandlingException);
    }

    @Test
    @SneakyThrows
    public void When_ProcessHasAlreadyFailed_Expect_NonNackTaskNotToBeExecuted() {
        String conversationId = "conversationId1";
        setupValidMessage(conversationId, TaskType.SEND_EHR_CONTINUE);
        when(processFailureHandlingService.hasProcessFailed(any())).thenReturn(true);

        var result = taskHandler.handle(message);

        assertThat(result).isTrue();
        verify(processFailureHandlingService).hasProcessFailed(conversationId);
        verifyNoInteractions(taskExecutor);
    }

    @Test
    @SneakyThrows
    public void When_ProcessHasAlreadyFailed_Expect_NackTaskToStillBeExecuted() {
        String conversationId = "conversationId1";
        setupValidMessage(conversationId, TaskType.SEND_NEGATIVE_ACKNOWLEDGEMENT);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        when(processFailureHandlingService.hasProcessFailed(any())).thenReturn(true);

        var result = taskHandler.handle(message);

        assertThat(result).isTrue();
        verify(processFailureHandlingService).hasProcessFailed(conversationId);
        verify(taskExecutor).execute(taskDefinition);
    }

    private String setupValidMessage(String conversationId, TaskType taskType) throws JMSException {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn("taskType");
        when(message.getBody(String.class)).thenReturn("body");
        when(taskDefinitionFactory.getTaskDefinition("taskType", "body")).thenReturn(taskDefinition);
        when(taskDefinition.getTaskType()).thenReturn(taskType);
        when(taskDefinition.getConversationId()).thenReturn(conversationId);
        return conversationId;
    }
}
