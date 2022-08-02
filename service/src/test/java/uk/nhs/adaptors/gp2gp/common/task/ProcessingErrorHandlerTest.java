package uk.nhs.adaptors.gp2gp.common.task;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_STRUCTURED;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;

@ExtendWith(MockitoExtension.class)
public class ProcessingErrorHandlerTest {

    @Mock
    private TaskDefinition taskDefinition;

    @Mock
    private ProcessFailureHandlingService processFailureHandlingService;

    @InjectMocks
    private ProcessingErrorHandler processingErrorHandler;

    @Test
    public void When_HandleRequestError_WithValidTask_Expect_ProcessToBeFailedWithCorrectCode() {

        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        processingErrorHandler.handleRequestError(taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("18"),
            eq("An error occurred processing the initial EHR request"),
            any());
    }

    @Test
    public void When_HandleRequestError_WithValidTask_Expect_ReturnValueOfFailService() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(processingErrorHandler.handleRequestError(taskDefinition));
        assertFalse(processingErrorHandler.handleRequestError(taskDefinition));
    }

    @Test
    public void When_HandleTranslationError_WithValidTask_Expect_ProcessToBeFailedWithCorrectCode() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        processingErrorHandler.handleTranslationError(taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("10"),
            eq("An error occurred translating the EHR extract"),
            any());
    }

    @Test
    public void When_HandleTranslationError_WithValidTask_Expect_ReturnValueOfFailService() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(processingErrorHandler.handleTranslationError(taskDefinition));
        assertFalse(processingErrorHandler.handleTranslationError(taskDefinition));
    }

    @Test
    public void When_HandleGeneralProcessingError_WithValidTask_Expect_ProcessToBeFailedWithCorrectCode() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        processingErrorHandler.handleGeneralProcessingError(taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("99"),
            eq("An error occurred when executing a task"),
            any());
    }

    @Test
    public void When_HandleGeneralProcessingError_WithValidTask_Expect_ReturnValueOfFailService() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(processingErrorHandler.handleGeneralProcessingError(taskDefinition));
        assertFalse(processingErrorHandler.handleGeneralProcessingError(taskDefinition));
    }

    @Test
    public void When_HandleGpConnectError_WithValidTask_Expect_ProcessToBeFailedWithCorrectCode() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        processingErrorHandler.handleGpConnectError(taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("20"),
            eq("An error occurred communicating with GP connect"),
            any());
    }

    @Test
    public void When_HandleGpConnectError_WithValidTask_Expect_ReturnValueOfFailService() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(processingErrorHandler.handleGpConnectError(taskDefinition));
        assertFalse(processingErrorHandler.handleGpConnectError(taskDefinition));
    }

    @Test
    public void When_HandleGeneralProcessingError_WithNullParameter_Expect_ProcessIsNotFailed() {
        processingErrorHandler.handleGeneralProcessingError(null);

        verifyNoInteractions(processFailureHandlingService);
    }

    @Test
    @SneakyThrows
    public void When_FailProcessThrowsException_Expect_ExceptionToBeRethrown() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);

        var failureHandlingException = new RuntimeException("failure handler exception");
        doThrow(failureHandlingException).when(processFailureHandlingService).failProcess(
            any(), any(), any(), any());

        assertThatThrownBy(
            () -> processingErrorHandler.handleGeneralProcessingError(taskDefinition)
        ).isSameAs(failureHandlingException);
    }
}
