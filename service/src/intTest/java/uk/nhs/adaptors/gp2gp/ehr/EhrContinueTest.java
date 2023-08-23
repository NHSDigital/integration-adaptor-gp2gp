package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.exception.NonExistingInteractionIdException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_CONTENT_TYPE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_NAME;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrContinueTest {
    private final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();

    private static final String CONTINUE_ACKNOWLEDGEMENT = "Continue Acknowledgement";


    @Autowired
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @MockBean
    private TaskDispatcher taskDispatcher;

    @Test
    public void When_EhrContinueIsValid_Expect_TaskDispatcherCalledWithSameValues() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        var expectedResponse = createContinueTasks(ehrExtractStatus);

        ehrExtractStatusRepository.save(ehrExtractStatus);
        ehrExtractRequestHandler.handleContinue(ehrExtractStatus.getConversationId(), CONTINUE_ACKNOWLEDGEMENT);

        verify(taskDispatcher).createTask(
            argThat((SendDocumentTaskDefinition task) -> {
                assertThat(task).usingRecursiveComparison().ignoringFields("taskId").isEqualTo(expectedResponse);
                return true;
            }));

        var ehrExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());
        assertThat(ehrExtract.get().getEhrContinue().getReceived()).isNotNull();
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

        Exception exception = assertThrows(NonExistingInteractionIdException.class,
            () -> ehrExtractRequestHandler.handleContinue(conversationId, CONTINUE_ACKNOWLEDGEMENT));

        assertThat(exception.getMessage()).isEqualTo("Received a Continue message that is not recognised with conversation_id: '"
            + conversationId + "'");
        verify(taskDispatcher, never()).createTask(any());
    }

    private SendDocumentTaskDefinition createContinueTasks(EhrExtractStatus ehrExtractStatus) {
        return SendDocumentTaskDefinition.builder()
            .documentName(DOCUMENT_NAME)
            .documentId(DOCUMENT_ID)
            .documentContentType(DOCUMENT_CONTENT_TYPE)
            .messageId(CONVERSATION_ID)
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .build();
    }
}
