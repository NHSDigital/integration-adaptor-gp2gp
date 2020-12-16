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
    @Autowired
    private TaskExecutorFactory taskExecutorFactory;

    private static Stream<Arguments> provideTasksDataForTest() {
        return Stream.of(
            Arguments.of(GetGpcDocumentTaskDefinition.class, GetGpcDocumentTaskDefinition.class.toString()),
            Arguments.of(GetGpcStructuredTaskDefinition.class, GetGpcStructuredTaskDefinition.class.toString())
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForTest")
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask(Class taskClass, String taskDefinitionClassString) {
        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinitionClassString);
        assertThat(taskExecutor.getTaskType()).isEqualTo(taskClass);
    }
}
