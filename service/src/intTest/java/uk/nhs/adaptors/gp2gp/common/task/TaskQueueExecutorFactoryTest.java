package uk.nhs.adaptors.gp2gp.common.task;

import static java.lang.Thread.sleep;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
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

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@SpringBootTest
@ExtendWith({MongoDBExtension.class, ActiveMQExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TaskQueueExecutorFactoryTest {
    private static final String DOCUMENT_TASK_DEFINITION_CLASS_VALUE = GetGpcDocumentTaskDefinition.class.toString();
    private static final String STRUCTURE_TASK_DEFINITION_CLASS_VALUE = GetGpcStructuredTaskDefinition.class.toString();

    @Value("${gp2gp.amqp.taskQueueName}")
    private String taskQueueName;
    @Autowired
    private JmsTemplate jmsTemplate;
    @MockBean
    private TaskExecutorFactory taskExecutorFactory;

    private int counter = 1;

    private static Stream<Arguments> provideTasksDataForExecutorTest() {
        return Stream.of(
            Arguments.of(DOCUMENT_OBJECT, DOCUMENT_TASK, GET_GPC_DOCUMENT_TASK_EXECUTOR, DOCUMENT_TASK_DEFINITION_CLASS_VALUE),
            Arguments.of(STRUCTURE_OBJECT, STRUCTURE_TASK, GET_GPC_STRUCTURED_TASK_EXECUTOR, STRUCTURE_TASK_DEFINITION_CLASS_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForExecutorTest")
    public void When_SendingValidMessage_Expect_TaskExecutorFactoryHasCorrectTaskDefinition(Object taskTestObject, String taskName,
        TaskExecutor taskTestExecutor, String expectedTaskClass) throws InterruptedException {
        when(taskExecutorFactory.getTaskExecutor(any())).thenReturn(taskTestExecutor);
        doNothing().when(taskTestExecutor).execute(any());

        jmsTemplate.convertAndSend(taskQueueName, taskTestObject, message -> {
            message.setStringProperty(TASK_NAME_VALUE, taskName);
            return message;
        });

        sleep(TIMEOUT);

        verify(taskExecutorFactory, times(counter)).getTaskExecutor(
            argThat(taskDefinitionClass -> hasSameContentAsSentTaskDefinitionClass(taskDefinitionClass, expectedTaskClass))
        );

        reset(taskExecutorFactory);
        counter++;
    }

    @SneakyThrows
    public boolean hasSameContentAsSentTaskDefinitionClass(String taskDefinitionClass, String expectedTaskClass) {
        return taskDefinitionClass.equals(expectedTaskClass);
    }
}

