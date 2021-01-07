package uk.nhs.adaptors.gp2gp.gpc;

import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.spy;

public class TaskExecutorFactoryTest {

    @Test
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask() throws TaskHandlerException {
        var getStructuredExecutor = spy(GetGpcStructuredTaskExecutor.class);
        var getDocumentExecutor = spy(GetGpcDocumentTaskExecutor.class);
        List<TaskExecutor> executors = List.of(getStructuredExecutor, getDocumentExecutor);
        TaskExecutorFactory taskExecutorFactory = new TaskExecutorFactory(executors);

        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcStructuredTaskDefinition.class)).isEqualTo(getStructuredExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcDocumentTaskDefinition.class)).isEqualTo(getDocumentExecutor);
        assertThatThrownBy(() -> {
            taskExecutorFactory.getTaskExecutor(NoExecutorTaskDefinition.class);
        })
            .isInstanceOf(TaskHandlerException.class)
            .hasMessage("No task executor class for task definition class '" + NoExecutorTaskDefinition.class + "'");
    }

    @SuperBuilder
    private static class NoExecutorTaskDefinition extends TaskDefinition {
        @Override
        public TaskType getTaskType() {
            return null;
        }
    }
}
