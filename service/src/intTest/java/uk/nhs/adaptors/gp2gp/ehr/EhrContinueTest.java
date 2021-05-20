package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrContinueTest {
    private final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();

    private static final String CONTINUE_ACKNOWLEDGEMENT = "Continue Acknowledgement";
    private static final String DOCUMENT_NAME = "documentName";

    @Autowired
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @MockBean
    private TaskDispatcher taskDispatcher;

    @Test
    public void When_EhrContinueIsValid_Expect_TaskDispatcherCalledWithSameValues() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        var expectedResponse = createContinueTasks(ehrExtractStatus);

        ehrExtractStatusRepository.save(ehrExtractStatus);
        ehrExtractRequestHandler.handleContinue(ehrExtractStatus.getConversationId(), CONTINUE_ACKNOWLEDGEMENT);

        verify(taskDispatcher).createTask(
            argThat(task -> hasSameContent(
                (SendEhrCommonTaskDefinition) task, expectedResponse)));
        var ehrExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());

        assertThat(ehrExtract.get().getEhrContinue().getReceived()).isNotNull();
    }

    @Test
    public void When_EhrContinueIsRunTwice_Expect_DataIsOverwritten() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        var expectedResponse = createContinueTasks(ehrExtractStatus);

        ehrExtractStatusRepository.save(ehrExtractStatus);
        ehrExtractRequestHandler.handleContinue(ehrExtractStatus.getConversationId(), CONTINUE_ACKNOWLEDGEMENT);

        verify(taskDispatcher).createTask(
            argThat(task -> hasSameContent(
                (SendEhrCommonTaskDefinition) task, expectedResponse)));
        var first = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        ehrExtractRequestHandler.handleContinue(ehrExtractStatus.getConversationId(), CONTINUE_ACKNOWLEDGEMENT);

        verify(taskDispatcher, times(2)).createTask(
            argThat(task -> hasSameContent(
                (SendEhrCommonTaskDefinition) task, expectedResponse)));
        var second = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(first.getEhrContinue().getReceived()).isBefore(second.getEhrContinue().getReceived());
    }

    @Test
    public void When_EhrContinueHasNoContinueAcknowledgement_Expect_InvalidInboundException() {
        String conversationId = randomIdGeneratorService.createNewId();
        String payload = "invalid payload";

        Exception exception = assertThrows(InvalidInboundMessageException.class, () ->
            ehrExtractRequestHandler.handleContinue(conversationId, payload));

        assertThat(exception.getMessage()).isEqualTo("Continue Message did not have Continue Acknowledgment, conversationId: "
            + conversationId);
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_EhrContinueThrowsException_Expect_EhrExtractStatusNotUpdated() {
        String conversationId = randomIdGeneratorService.createNewId();

        Exception exception = assertThrows(EhrExtractException.class,
            () -> ehrExtractRequestHandler.handleContinue(conversationId, CONTINUE_ACKNOWLEDGEMENT));

        assertThat(exception.getMessage()).isEqualTo("Received a Continue message with a Conversation-Id '" + conversationId
            + "' that is not recognised");
        verify(taskDispatcher, never()).createTask(any());
    }

    private SendEhrCommonTaskDefinition createContinueTasks(EhrExtractStatus ehrExtractStatus) {
        return SendEhrCommonTaskDefinition.builder()
            .documentName(DOCUMENT_NAME)
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .build();
    }

    private boolean hasSameContent(SendEhrCommonTaskDefinition structuredTaskDefinition, SendEhrCommonTaskDefinition expectedResponse) {
        assertThat(structuredTaskDefinition.getConversationId()).isEqualTo(expectedResponse.getConversationId());
        assertThat(structuredTaskDefinition.getFromAsid()).isEqualTo(expectedResponse.getFromAsid());
        assertThat(structuredTaskDefinition.getFromOdsCode()).isEqualTo(expectedResponse.getFromOdsCode());
        assertThat(structuredTaskDefinition.getRequestId()).isEqualTo(expectedResponse.getRequestId());
        assertThat(structuredTaskDefinition.getToOdsCode()).isEqualTo(expectedResponse.getToOdsCode());

        return true;
    }
}
