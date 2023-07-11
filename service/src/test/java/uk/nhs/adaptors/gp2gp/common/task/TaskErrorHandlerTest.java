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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;

@ExtendWith(MockitoExtension.class)
public class TaskErrorHandlerTest {
    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception";

    @Mock
    private TaskDefinition taskDefinition;

    @Mock
    private ProcessFailureHandlingService processFailureHandlingService;

    @InjectMocks
    private TaskErrorHandler taskErrorHandler;

    @BeforeEach
    public void setup() {
        when(taskDefinition.getTaskType()).thenReturn(GET_GPC_STRUCTURED);
    }

    @Test
    public void When_HandleProcessingError_WithEhrRequestException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new EhrRequestException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("18"),
            eq("Request message not well-formed or not able to be processed"),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithEhrRequestException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new EhrRequestException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new EhrRequestException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_With_EhrExtractException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new EhrExtractException("Test Exception"), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("10"),
            eq("Failed to successfully generate EHR Extract."),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithEhrExtractException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new EhrExtractException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new EhrExtractException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithEhrMapperException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new EhrMapperException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("10"),
            eq("Failed to successfully generate EHR Extract."),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithEhrMapperException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new EhrMapperException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new EhrMapperException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithFhirValidationException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new FhirValidationException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("10"),
            eq("Failed to successfully generate EHR Extract."),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithFhirValidationException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new FhirValidationException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new FhirValidationException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithOtherException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new Exception(), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("99"),
            eq("An error occurred when executing a task"),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithOtherException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new Exception(), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new Exception(), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new GpConnectException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("20"),
            eq("Spine system responded with an error"),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new GpConnectException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new GpConnectException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectInvalidException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new GpConnectInvalidException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("19"),
            eq("Sender check indicates that Requester is not the patient’s current healthcare provider"),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectInvalidException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new GpConnectInvalidException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new GpConnectInvalidException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectGpConnectNotFoundException_Expect_ProcessToBeFailedWithCorrectCode() {
        taskErrorHandler.handleProcessingError(new GpConnectNotFoundException(TEST_EXCEPTION_MESSAGE), taskDefinition);

        verify(processFailureHandlingService).failProcess(
            any(),
            eq("06"),
            eq("Patient not at surgery."),
            any());
    }

    @Test
    public void When_HandleProcessingError_WithGpConnectNotFoundException_Expect_ReturnValueOfFailService() {
        when(processFailureHandlingService.failProcess(any(), any(), any(), any()))
            .thenReturn(true, false);

        assertTrue(taskErrorHandler.handleProcessingError(new GpConnectNotFoundException(TEST_EXCEPTION_MESSAGE), taskDefinition));
        assertFalse(taskErrorHandler.handleProcessingError(new GpConnectNotFoundException(TEST_EXCEPTION_MESSAGE), taskDefinition));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void When_HandleGeneralProcessingError_WithNullParameter_Expect_ProcessIsNotFailed() {
        taskErrorHandler.handleProcessingError(new RuntimeException(), null);

        verifyNoInteractions(processFailureHandlingService);
    }
    @Test
    @SneakyThrows
    public void When_FailProcessThrowsException_Expect_ExceptionToBeRethrown() {
        var failureHandlingException = new RuntimeException("failure handler exception");
        doThrow(failureHandlingException).when(processFailureHandlingService).failProcess(
            any(), any(), any(), any());

        assertThatThrownBy(
            () -> taskErrorHandler.handleProcessingError(new RuntimeException(), taskDefinition)
        ).isSameAs(failureHandlingException);
    }
}
