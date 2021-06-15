package uk.nhs.adaptors.gp2gp.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.SendNegativeAcknowledgementTaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class ProcessFailureHandlingServiceTest {

    @Mock
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @Mock
    private SendNegativeAcknowledgementTaskDispatcher sendNackTaskDispatcher;

    @Mock
    private EhrExtractStatus ehrExtractStatus;

    @Mock
    private EhrExtractStatus.Error ehrExtractStatusError;

    @InjectMocks
    private ProcessFailureHandlingService processFailureHandlingService;

    @Test
    public void When_EhrExtractStatusExistsInDB_Expect_FailProcessToReturnTrue() {
        var conversationId = "conversationId1";
        var errorCode = "errorCode1";
        var errorMessage = "errorMessage1";
        var taskType = "taskType1";

        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.failProcess(conversationId, errorCode, errorMessage, taskType);

        assertThat(result).isTrue();
        verify(ehrExtractStatusRepository).findByConversationId(conversationId);
        verify(ehrExtractStatusService).updateEhrExtractStatusError(conversationId, errorCode, errorMessage, taskType);
        verify(sendNackTaskDispatcher).sendNegativeAcknowledgement(ehrExtractStatus, errorCode, errorMessage);
    }

    @Test
    public void When_EhrExtractStatusNotInDB_Expect_FailProcessToReturnFalse() {
        doReturn(Optional.empty()).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.failProcess("convId1", "errorCode1", "errorMsg1", "taskType1");

        assertThat(result).isFalse();
        verifyNoInteractions(ehrExtractStatusService, sendNackTaskDispatcher);
    }

    @Test
    public void When_ExceptionIsThrown_Expect_FailProcessToReturnFalse() {
        doThrow(new RuntimeException("test exception")).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.failProcess("convId1", "errorCode1", "errorMsg1", "taskType1");

        assertThat(result).isFalse();
    }

    @Test
    public void When_EhrExtractStatusNotInDB_Expect_HasProcessFailedToReturnFalse() {
        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.hasProcessFailed("conversationId1");

        assertThat(result).isFalse();
    }

    @Test
    public void When_ErrorInEhrExtractStatusIsMissing_Expect_HasProcessFailedToReturnFalse() {
        doReturn(null).when(ehrExtractStatus).getError();
        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.hasProcessFailed("conversationId1");

        assertThat(result).isFalse();
    }

    @Test
    public void When_ErrorInEhrExtractStatusIsPresent_Expect_HasProcessFailedToReturnTrue() {
        doReturn(ehrExtractStatusError).when(ehrExtractStatus).getError();
        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(any());

        var result = processFailureHandlingService.hasProcessFailed("conversationId1");

        assertThat(result).isTrue();
    }

    @Test
    public void When_EhrExtractStatusRepositoryFails_Expect_HasProcessFailedToThrowException() {
        var expectedException = new RuntimeException("test exception");
        doThrow(expectedException).when(ehrExtractStatusRepository).findByConversationId(any());

        assertThatThrownBy(() ->
            processFailureHandlingService.hasProcessFailed("conversationId1")
        ).isSameAs(expectedException);
    }
}
