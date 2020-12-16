package uk.nhs.adaptors.gp2gp.common.task;

import static java.lang.Thread.sleep;
import static java.util.Optional.of;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.constants.Constants.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.constants.Constants.STRUCTURE_TASK;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.DOCUMENT_OBJECT;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.GET_GPC_DOCUMENT_TASK_EXECUTOR;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.GET_GPC_STRUCTURED_TASK_EXECUTOR;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.STRUCTURE_OBJECT;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.TASK_NAME_VALUE;
import static uk.nhs.adaptors.gp2gp.common.task.constants.TaskConstants.TIMEOUT;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinitionFactory;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TaskQueueToDefinitionFactoryTest {
    private static final Optional<GetGpcDocumentTaskDefinition> GET_GPC_DOCUMENT_TASK_DEFINITION =
        of(mock(GetGpcDocumentTaskDefinition.class));
    private static final Optional<GetGpcStructuredTaskDefinition> GET_GPC_STRUCTURED_TASK_DEFINITION =
        of(mock(GetGpcStructuredTaskDefinition.class));
    private static final String DOCUMENT_EXPECTED_BODY = "{\"requestId\":\"123\",\"conversationId\":\"456\",\"documentId\":\"789\"}";
    private static final String STRUCTURE_EXPECTED_BODY = "{\"requestId\":\"123\",\"conversationId\":\"456\",\"nhsNumber\":\"789\"}";

    @Value("${gp2gp.amqp.taskQueueName}")
    private String taskQueueName;
    @Autowired
    private JmsTemplate jmsTemplate;
    @MockBean
    private TaskDefinitionFactory taskDefinitionFactory;
    @MockBean
    private TaskExecutorFactory taskExecutorFactory;

    private int counter = 1;

    private static Stream<Arguments> provideTasksDataForDefinitionTest() {
        return Stream.of(
            Arguments.of(DOCUMENT_OBJECT, DOCUMENT_TASK, GET_GPC_DOCUMENT_TASK_EXECUTOR, GET_GPC_DOCUMENT_TASK_DEFINITION,
                DOCUMENT_EXPECTED_BODY),
            Arguments.of(STRUCTURE_OBJECT, STRUCTURE_TASK, GET_GPC_STRUCTURED_TASK_EXECUTOR, GET_GPC_STRUCTURED_TASK_DEFINITION,
                STRUCTURE_EXPECTED_BODY)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForDefinitionTest")
    public void When_SendingValidMessage_Expect_TaskDefinitionFactoryCalledWithSameMessageAndTask(Object taskTestObject, String taskName,
        TaskExecutor taskTestExecutor, Optional<TaskDefinition> taskDefinition, String expectedBody)
        throws InterruptedException, JsonProcessingException {

        when(taskDefinitionFactory.getTaskDefinition(any(), any())).thenReturn(taskDefinition);
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskTestExecutor);
        doNothing().when(taskTestExecutor).execute(any());

        jmsTemplate.convertAndSend(taskQueueName, taskTestObject, message -> {
            message.setStringProperty(TASK_NAME_VALUE, taskName);
            return message;
        });

        sleep(TIMEOUT);

        verify(taskDefinitionFactory, times(counter)).getTaskDefinition(
            argThat(actualTaskName -> hasSameContentAsSentTaskDefinition(actualTaskName, taskName)),
            argThat(actualBody -> hasSameContentAsSentTaskDefinition(actualBody, expectedBody))
        );

        reset(taskDefinitionFactory);
        reset(taskExecutorFactory);
        counter++;
    }

    @SneakyThrows
    public boolean hasSameContentAsSentTaskDefinition(String taskDefinitionClass, String expectedTaskClass) {
        return taskDefinitionClass.equals(expectedTaskClass);
    }
}
