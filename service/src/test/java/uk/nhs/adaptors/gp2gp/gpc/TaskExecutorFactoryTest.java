package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.experimental.SuperBuilder;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutorFactory;
import uk.nhs.adaptors.gp2gp.common.task.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementExecutor;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskExecutor;

@ExtendWith(MockitoExtension.class)
public class TaskExecutorFactoryTest {
    @Spy
    @InjectMocks
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    @Spy
    @InjectMocks
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;
    @Spy
    @InjectMocks
    private SendAcknowledgementExecutor sendAcknowledgementExecutor;
    @Spy
    @InjectMocks
    private SendEhrExtractCoreTaskExecutor sendEhrExtractCoreTaskExecutor;
    @Spy
    @InjectMocks
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;

    @Test
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask() throws TaskHandlerException {
        List<TaskExecutor> executors = List.of(
            getGpcDocumentTaskExecutor,
            getGpcStructuredTaskExecutor,
            sendAcknowledgementExecutor,
            sendEhrExtractCoreTaskExecutor,
            sendDocumentTaskExecutor
        );

        TaskExecutorFactory taskExecutorFactory = new TaskExecutorFactory(executors);

        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcStructuredTaskDefinition.class)).isEqualTo(getGpcStructuredTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcDocumentTaskDefinition.class)).isEqualTo(getGpcDocumentTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(SendAcknowledgementTaskDefinition.class)).isEqualTo(sendAcknowledgementExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(SendEhrExtractCoreTaskDefinition.class)).isEqualTo(sendEhrExtractCoreTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(SendDocumentTaskDefinition.class)).isEqualTo(sendDocumentTaskExecutor);

        assertThatThrownBy(() -> {
            taskExecutorFactory.getTaskExecutor(NoExecutorTaskDefinition.class);
        })
            .isInstanceOf(TaskHandlerException.class)
            .hasMessage("No task executor class for task definition class '" + NoExecutorTaskDefinition.class + "'");
    }

    @SuperBuilder
    private static final class NoExecutorTaskDefinition extends TaskDefinition {
        @Override
        public TaskType getTaskType() {
            return null;
        }
    }
}
