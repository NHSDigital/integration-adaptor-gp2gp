package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;

import javax.jms.Message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.task.TaskConsumer;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class TaskHandlingTest {

    private static final String TASK_TYPE_HEADER_NAME = "TaskType";
    private static final String NACK_TYPE_CODE = "AE";

    @SpyBean
    private ObjectMapper objectMapper;
    @Autowired
    private TaskConsumer taskConsumer;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @SpyBean
    private EhrExtractStatusService ehrExtractStatusService;
    @SpyBean
    private TaskExecutorFactory taskExecutorFactory;
    @MockBean
    private TaskDispatcher taskDispatcher;
    @Mock
    private Message message;
    @Mock
    private TaskExecutor taskExecutor;


    @BeforeEach
    public void setup() {
        lenient().doReturn(taskExecutor).when(taskExecutorFactory).getTaskExecutor(any());
    }

    @Test
    @SneakyThrows
    public void When_MessageIsUnreadable_Expect_MessageNotToBeProcessed() {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn(SendEhrExtractCoreTaskDefinition.class.getName());
        when(message.getBody(any())).thenReturn("not a valid body");

        taskConsumer.receive(message);

        verify(message, never()).acknowledge();
        verifyNoInteractions(taskExecutorFactory, ehrExtractStatusService);
    }

    @Test
    @SneakyThrows
    public void When_NonNackTaskProcessingFails_Expect_WholeProcessToBeFailed() {
        mockSendEhExtractCoreTaskMessage();
        doThrow(RuntimeException.class).when(taskExecutor).execute(any());
        var ehrExtractStatus = createEhrExtractStatusInDb(false);

        taskConsumer.receive(message);

        verify(message).acknowledge();

        assertThatSendNackTaskHasBeenTriggered();
        assertConversationIsFailed(ehrExtractStatus);
    }

    @Test
    @SneakyThrows
    public void When_NackTaskProcessingFails_Expect_MessageNotToBeAcknowledged() {
        mockSendNackTaskMessage();
        EhrExtractStatus ehrExtractStatus = createEhrExtractStatusInDb(true);
        doThrow(RuntimeException.class).when(taskExecutor).execute(any());
        var initialDbStatus = readEhrExtractStatusFromDb();

        taskConsumer.receive(message);

        verify(message, never()).acknowledge();

        verifyNoInteractions(taskDispatcher);
        assertConversationIsFailed(ehrExtractStatus);

        var finalDbStatus = readEhrExtractStatusFromDb();
        assertThat(finalDbStatus).usingRecursiveComparison().isEqualTo(initialDbStatus);
    }

    @Test
    @SneakyThrows
    public void When_ProcessIsAlreadyFailed_Expect_NonNackTaskToBeAborted() {
        mockSendEhExtractCoreTaskMessage();
        EhrExtractStatus ehrExtractStatus = createEhrExtractStatusInDb(true);
        var initialDbExtract = readEhrExtractStatusFromDb();

        assertThat(initialDbExtract.getError()).isNotNull();

        taskConsumer.receive(message);

        verify(message).acknowledge();
        verifyNoInteractions(taskExecutor);

        var finalDbStatus = readEhrExtractStatusFromDb();
        assertThat(finalDbStatus).usingRecursiveComparison().isEqualTo(initialDbExtract);
    }

    @Test
    @SneakyThrows
    public void When_ProcessIsAlreadyFailed_Expect_NackTaskToStillBeExecuted() {
        var taskDefinition = mockSendNackTaskMessage();
        EhrExtractStatus ehrExtractStatus = createEhrExtractStatusInDb(true);
        var initialDbStatus = readEhrExtractStatusFromDb();

        taskConsumer.receive(message);

        verify(message).acknowledge();
        verify(taskExecutor).execute(taskDefinition);
        assertConversationIsFailed(ehrExtractStatus);

        var finalDbStatus = readEhrExtractStatusFromDb();
        assertThat(finalDbStatus).usingRecursiveComparison().isEqualTo(initialDbStatus);
    }

    @Test
    @SneakyThrows
    public void When_ProcessIsNotFailed_Expect_TaskToBeExecuted() {
        var taskDefinition = mockSendEhExtractCoreTaskMessage();
        createEhrExtractStatusInDb(false);

        taskConsumer.receive(message);

        verify(message).acknowledge();
        verify(taskExecutor).execute(taskDefinition);

        var finalDbStatus = readEhrExtractStatusFromDb();
        assertThat(finalDbStatus.getError()).isNull();
    }

    private EhrExtractStatus readEhrExtractStatusFromDb() {
        return ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID).get();
    }

    private EhrExtractStatus createEhrExtractStatusInDb(boolean addError) {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();

        if (addError) {
            ehrExtractStatus.setError(EhrExtractStatus.Error.builder().build());
        }

        ehrExtractStatusRepository.save(ehrExtractStatus);

        return ehrExtractStatus;
    }

    private void assertConversationIsFailed(EhrExtractStatus ehrExtractStatus) {
        var dbExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(dbExtract.getError()).isNotNull();
    }

    private void assertThatSendNackTaskHasBeenTriggered() {
        var ackDefinitionCaptor = ArgumentCaptor.forClass(SendAcknowledgementTaskDefinition.class);
        verify(taskDispatcher).createTask(ackDefinitionCaptor.capture());
        verifyNoMoreInteractions(taskDispatcher);

        assertThat(ackDefinitionCaptor.getValue().isNack()).isTrue();
    }

    @SneakyThrows
    private SendAcknowledgementTaskDefinition mockSendNackTaskMessage() {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn(SendAcknowledgementTaskDefinition.class.getName());

        var taskDefinition =
            SendAcknowledgementTaskDefinition
                .builder()
                .conversationId(CONVERSATION_ID)
                .typeCode(NACK_TYPE_CODE).build();

        doReturn(taskDefinition).when(objectMapper).readValue(
            ArgumentMatchers.<String>any(),
            ArgumentMatchers.<Class<SendAcknowledgementTaskDefinition>>any()
        );

        return taskDefinition;
    }

    @SneakyThrows
    private SendEhrExtractCoreTaskDefinition mockSendEhExtractCoreTaskMessage() {
        when(message.getStringProperty(TASK_TYPE_HEADER_NAME)).thenReturn(SendEhrExtractCoreTaskDefinition.class.getName());

        var taskDefinition =
            SendEhrExtractCoreTaskDefinition.builder().conversationId(CONVERSATION_ID).build();

        doReturn(taskDefinition).when(objectMapper).readValue(
            ArgumentMatchers.<String>any(),
            ArgumentMatchers.<Class<SendEhrExtractCoreTaskDefinition>>any()
        );

        return taskDefinition;
    }
}
