package uk.nhs.adaptors.gp2gp.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TaskExecutorFactoryTest {
    private static final GetGpcDocumentTaskDefinition GET_GPC_DOCUMENT_TASK_DEFINITION =
        new GetGpcDocumentTaskDefinition("", "", "");
    private static final GetGpcStructuredTaskDefinition GET_GPC_STRUCTURED_TASK_DEFINITION =
        new GetGpcStructuredTaskDefinition("", "", "");
    @Autowired
    private TaskExecutorFactory taskExecutorFactory;

    private static Stream<Arguments> provideTasksDataForTest() {
        return Stream.of(
            Arguments.of(GetGpcDocumentTaskDefinition.class, GET_GPC_DOCUMENT_TASK_DEFINITION),
            Arguments.of(GetGpcStructuredTaskDefinition.class, GET_GPC_STRUCTURED_TASK_DEFINITION)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForTest")
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask(Class taskClass, TaskDefinition taskDefinition) {
        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition);
        assertThat(taskExecutor.getTaskType()).isEqualTo(taskClass);
    }
}
