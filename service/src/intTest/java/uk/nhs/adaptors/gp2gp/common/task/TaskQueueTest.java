package uk.nhs.adaptors.gp2gp.common.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.constants.Constants.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.constants.Constants.STRUCTURE_TASK;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;

import lombok.Data;
import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TaskQueueTest {
    private static final long TIMEOUT = 5000L;
    private static final String TASK_NAME_VALUE = "TaskName";
    private static final TestDocumentObject DOCUMENT_OBJECT = new TestDocumentObject("123", "456", "789");
    private static final TestStructureObject STRUCTURE_OBJECT = new TestStructureObject("123", "456", "789");
    private static final GetGpcDocumentTaskExecutor GET_GPC_DOCUMENT_TASK_EXECUTOR = new GetGpcDocumentTaskExecutor();
    private static final GetGpcStructuredTaskExecutor GET_GPC_STRUCTURED_TASK_EXECUTOR = new GetGpcStructuredTaskExecutor();

    @Value("${gp2gp.amqp.taskQueueName}")
    private String taskQueueName;
    @Autowired
    private JmsTemplate jmsTemplate;
    @MockBean // mock the TaskExecutorFactory to validate input, further execution needs to continue or message will be retried times 3.
    private TaskExecutorFactory taskExecutorFactory;

    private int counter = 1;

    private static Stream<Arguments> provideTasksDataForTest() {
        return Stream.of(
            Arguments.of(DOCUMENT_OBJECT, DOCUMENT_TASK, GET_GPC_DOCUMENT_TASK_EXECUTOR),
            Arguments.of(STRUCTURE_OBJECT, STRUCTURE_TASK, GET_GPC_STRUCTURED_TASK_EXECUTOR)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForTest")
    public void When_SendingValidMessage_Expect_TaskExecutorFactoryCalledWithSameMessage(Object testObject, String taskName,
        TaskExecutor taskExecutor) throws InterruptedException {
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskExecutor);

        jmsTemplate.convertAndSend(taskQueueName, testObject, message -> {
            message.setStringProperty(TASK_NAME_VALUE, taskName);
            return message;
        });

        Thread.sleep(TIMEOUT);

        verify(taskExecutorFactory, times(counter)).getTaskExecutor(
            argThat(taskDefinition -> hasSameContentAsSentTaskDefinition(taskDefinition, testObject))
        );

        Mockito.reset(taskExecutorFactory);
        counter++;
    }

    @SneakyThrows
    public boolean hasSameContentAsSentTaskDefinition(TaskDefinition taskDefinition, Object testObject) {
        if (testObject instanceof TestStructureObject) {
            TestStructureObject testStructureObject = (TestStructureObject) testObject;
            return taskDefinition.getConversationId().equals(testStructureObject.getConversationId())
                && taskDefinition.getRequestId().equals(testStructureObject.getRequestId());
        }
        if (testObject instanceof TestDocumentObject) {
            TestDocumentObject testDocumentObject = (TestDocumentObject) testObject;
            return taskDefinition.getConversationId().equals(testDocumentObject.getConversationId())
                && taskDefinition.getRequestId().equals(testDocumentObject.getRequestId());
        }
        return false;
    }
}

@Data
class TestDocumentObject {
    private String requestId;
    private String conversationId;
    private String documentId;

    TestDocumentObject(String requestId, String conversationId, String documentId) {
        this.documentId = documentId;
        this.conversationId = conversationId;
        this.requestId = requestId;
    }
}

@Data
class TestStructureObject {
    private String requestId;
    private String conversationId;
    private String nhsNumber;

    TestStructureObject(String requestId, String conversationId, String nhsNumber) {
        this.nhsNumber = nhsNumber;
        this.conversationId = conversationId;
        this.requestId = requestId;
    }
}
