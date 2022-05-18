package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskDefinition;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
public class TaskHandlerTest {

    private static final String CONVERSATION_ID = "conversationId1";

    @Mock
    private TaskDefinitionFactory taskDefinitionFactory;

    @Mock
    private TaskExecutorFactory taskExecutorFactory;

    @Mock
    private MDCService mdcService;

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private TaskHandler taskHandler;

    @Mock
    private Message message;

    @Mock
    private ProcessFailureHandlingService processFailureHandlingService;

    @Mock
    private ProcessingErrorHandler processingErrorHandler;

    private TaskDefinition taskDefinition;
    private SendAcknowledgementTaskDefinition sendAcknowledgementTaskDefinition;

    @Test
    @SneakyThrows
    public void When_TaskHandled_Expect_TaskExecuted() {
        setUpContinueMessage();

        var result = taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
        assertThat(result).isTrue();
    }

    @Test
    @SneakyThrows
    public void When_MessageIsUnreadable_Expect_MessageProcessingToBeAborted() {
        doThrow(JMSException.class).when(message).getStringProperty(TASK_TYPE_HEADER_NAME);

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
        verifyNoInteractions(
            taskDefinitionFactory,
            taskExecutorFactory,
            taskExecutor,
            processFailureHandlingService,
            processingErrorHandler
        );
    }

    @Test
    @SneakyThrows
    public void When_AnyExceptionIsThrownWhenReadingMessage_Expect_MessageProcessingToBeAborted() {
        doThrow(RuntimeException.class).when(message).getStringProperty(TASK_TYPE_HEADER_NAME);

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
        verifyNoInteractions(
            taskDefinitionFactory,
            taskExecutorFactory,
            taskExecutor,
            processFailureHandlingService
        );
    }

    @Test
    @SneakyThrows
    public void When_CannotRetrieveTaskDefinition_Expect_MessageProcessingToBeAborted() {
        var taskType = "taskType1";
        var messageBody = "body1";
        when(message.getStringProperty(any())).thenReturn(taskType);
        when(message.getBody(String.class)).thenReturn(messageBody);
        doThrow(TaskHandlerException.class).when(taskDefinitionFactory).getTaskDefinition(any(), any());

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();

        verify(taskDefinitionFactory).getTaskDefinition(taskType, messageBody);
        verifyNoInteractions(taskExecutorFactory, taskExecutor, processFailureHandlingService);
    }

    @Test
    @SneakyThrows
    public void When_NackTaskFails_Expect_ProcessNotToBeFailed() {
        setupAckMessage(SendAcknowledgementTaskDefinition.NACK_TYPE_CODE);
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());
        when(processingErrorHandler.handleGeneralProcessingError(any())).thenReturn(false);

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
        verify(taskExecutor).execute(sendAcknowledgementTaskDefinition);
        verify(processFailureHandlingService, never()).failProcess(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    public void When_NonNackTaskFails_Expect_ProcessToBeFailed() {
        setUpContinueMessage();
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
        verify(processingErrorHandler).handleGeneralProcessingError(any());
    }

    @Test
    @SneakyThrows
    public void When_OtherTaskFails_Expect_ResultFromErrorHandlerToBeReturned() {
        setUpContinueMessage();
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        when(processingErrorHandler.handleGeneralProcessingError(any()))
            .thenReturn(true, false);

        assertThat(taskHandler.handle(message)).isTrue();
        assertThat(taskHandler.handle(message)).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_NonAckTaskFails_Expect_ResultFromErrorHandlerToBeReturned() {
        setupAckMessage(SendAcknowledgementTaskDefinition.ACK_TYPE_CODE);
        doThrow(new RuntimeException("test exception")).when(taskExecutor).execute(any());

        when(processingErrorHandler.handleGeneralProcessingError(any()))
            .thenReturn(true, false);

        assertThat(taskHandler.handle(message)).isTrue();
        assertThat(taskHandler.handle(message)).isFalse();
    }


    @Test
    @SneakyThrows
    public void When_ErrorHandlerThrowsException_Expect_ExceptionToBeRethrown() {
        setUpContinueMessage();
        doThrow(new RuntimeException("task executor exception")).when(taskExecutor).execute(any());

        var failureHandlingException = new RuntimeException("failure handler exception");
        doThrow(failureHandlingException).when(processingErrorHandler).handleGeneralProcessingError(any());

        assertThatThrownBy(
            () -> taskHandler.handle(message)
        ).isSameAs(failureHandlingException);
    }

    @Test
    @SneakyThrows
    public void When_ProcessHasAlreadyFailed_Expect_NonNackTaskNotToBeExecuted() {
        setUpContinueMessage();
        when(processFailureHandlingService.hasProcessFailed(any())).thenReturn(true);

        var result = taskHandler.handle(message);

        assertThat(result).isTrue();
        verify(processFailureHandlingService).hasProcessFailed(CONVERSATION_ID);
        verifyNoInteractions(taskExecutor);
    }

    @Test
    @SneakyThrows
    public void When_ProcessHasAlreadyFailed_Expect_NackTaskToStillBeExecuted() {
        setupAckMessage(SendAcknowledgementTaskDefinition.NACK_TYPE_CODE);
        when(processFailureHandlingService.hasProcessFailed(any())).thenReturn(true);

        var result = taskHandler.handle(message);

        assertThat(result).isTrue();
        verify(processFailureHandlingService).hasProcessFailed(CONVERSATION_ID);
        verify(taskExecutor).execute(sendAcknowledgementTaskDefinition);
    }

    private void setupAckMessage(String typeCode) throws JMSException {
        lenient().when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn("taskType");
        when(message.getBody(String.class)).thenReturn("body");
        sendAcknowledgementTaskDefinition = SendAcknowledgementTaskDefinition.builder()
            .typeCode(typeCode)
            .conversationId(TaskHandlerTest.CONVERSATION_ID)
            .build();
        when(taskDefinitionFactory.getTaskDefinition("taskType", "body")).thenReturn(sendAcknowledgementTaskDefinition);
    }

    private void setUpContinueMessage() throws JMSException {
        lenient().when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn("taskType");
        when(message.getBody(String.class)).thenReturn("body");
        taskDefinition = SendDocumentTaskDefinition.builder().conversationId(CONVERSATION_ID).build();
        when(taskDefinitionFactory.getTaskDefinition("taskType", "body")).thenReturn(taskDefinition);
    }
}
