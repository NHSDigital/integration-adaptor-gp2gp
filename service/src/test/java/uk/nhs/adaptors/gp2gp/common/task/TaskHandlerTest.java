package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.dao.DataAccessResourceFailureException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.exception.GeneralProcessingException;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
public class TaskHandlerTest {

    private static final String CONVERSATION_ID = "conversationId1";
    private static final String TEST_EXCEPTION_MESSAGE = "Test exception";

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
    private TaskErrorHandler taskErrorHandler;

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
            taskErrorHandler
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
        Exception exception = new RuntimeException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());
        when(taskErrorHandler.handleProcessingError(eq(exception), any())).thenReturn(false);

        var result = taskHandler.handle(message);

        assertThat(result).isFalse();
        verify(taskExecutor).execute(sendAcknowledgementTaskDefinition);
        verify(processFailureHandlingService, never()).failProcess(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    public void When_NonNackTaskFails_Expect_ProcessToBeFailed() {
        setUpContinueMessage();
        Exception exception = new RuntimeException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        taskHandler.handle(message);

        verify(taskExecutor).execute(taskDefinition);
        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_OtherTaskFails_Expect_ResultFromErrorHandlerToBeReturned() {
        setUpContinueMessage();
        Exception exception = new RuntimeException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        when(taskErrorHandler.handleProcessingError(eq(exception), any()))
            .thenReturn(true, false);

        assertThat(taskHandler.handle(message)).isTrue();
        assertThat(taskHandler.handle(message)).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_NonAckTaskFails_Expect_ResultFromErrorHandlerToBeReturned() {
        setupAckMessage(SendAcknowledgementTaskDefinition.ACK_TYPE_CODE);
        Exception exception = new RuntimeException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        when(taskErrorHandler.handleProcessingError(eq(exception), any()))
            .thenReturn(true, false);

        assertThat(taskHandler.handle(message)).isTrue();
        assertThat(taskHandler.handle(message)).isFalse();
    }


    @Test
    @SneakyThrows
    public void When_ErrorHandlerThrowsException_Expect_ExceptionToBeRethrown() {
        setUpContinueMessage();
        Exception taskException = new RuntimeException("task executor exception");
        doThrow(taskException).when(taskExecutor).execute(any());

        var failureHandlingException = new RuntimeException("failure handler exception");
        doThrow(failureHandlingException).when(taskErrorHandler).handleProcessingError(eq(taskException), any());

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

    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrowsEhrExtractException_Expect_ErrorHandled() {
        setUpContinueMessage();
        Exception exception = new EhrExtractException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrowsEhrMapperException_Expect_ErrorHandled() {
        setUpContinueMessage();
        Exception exception = new EhrMapperException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrowFhirValidationException_Expect_ErrorHandled() {
        setUpContinueMessage();
        Exception exception = new FhirValidationException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }


    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrowsEhrRequestException_Expect_ErrorHandled() {
        setUpContinueMessage();
        Exception exception = new EhrRequestException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrowsGeneralProcessingException_Expect_ErrorHandled() {
        setUpContinueMessage();
        Exception exception = new GeneralProcessingException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithExecuteThrows_MhsConnectionException_Expect_ExceptionThrown() {
        setUpContinueMessage();
        doThrow(new MhsConnectionException("test exception")).when(taskExecutor).execute(any());

        assertThatExceptionOfType(MhsConnectionException.class).isThrownBy(() -> taskHandler.handle(message));
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithDataAccessResourceFailureException_Expect_ExceptionThrown() {
        setUpContinueMessage();
        doThrow(new DataAccessResourceFailureException("test exception")).when(processFailureHandlingService).hasProcessFailed(any());

        assertThatExceptionOfType(DataAccessResourceFailureException.class).isThrownBy(() -> taskHandler.handle(message));
    }

    @Test
    @SneakyThrows
    public void When_Handle_WithMhsServerErrorException_Expect_ProcessFailed() {
        setUpContinueMessage();
        Exception exception = new MhsServerErrorException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());

    }

    @Test
    @SneakyThrows
    public void When_handle_WithGpConnectInvalidException_Expect_ProcessFailed() {
        setUpContinueMessage();
        Exception exception = new GpConnectInvalidException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
    }

    @Test
    @SneakyThrows
    public void When_handle_WithGpConnectNotFoundException_Expect_ProcessFailed() {
        setUpContinueMessage();
        Exception exception = new GpConnectNotFoundException(TEST_EXCEPTION_MESSAGE);
        doThrow(exception).when(taskExecutor).execute(any());

        var result = taskHandler.handle(message);
        assertThat(result).isFalse();

        verify(taskErrorHandler).handleProcessingError(eq(exception), any());
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
