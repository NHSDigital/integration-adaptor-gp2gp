package uk.nhs.adaptors.gp2gp.gpc;

import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.task.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TaskExecutorFactoryTest {
    @Spy
    @InjectMocks
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    @Spy
    @InjectMocks
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;

    @Test
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask() throws TaskHandlerException {
        List<TaskExecutor> executors = List.of(getGpcDocumentTaskExecutor, getGpcStructuredTaskExecutor);
        TaskExecutorFactory taskExecutorFactory = new TaskExecutorFactory(executors);

        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcStructuredTaskDefinition.class)).isEqualTo(getGpcStructuredTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcDocumentTaskDefinition.class)).isEqualTo(getGpcDocumentTaskExecutor);
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
