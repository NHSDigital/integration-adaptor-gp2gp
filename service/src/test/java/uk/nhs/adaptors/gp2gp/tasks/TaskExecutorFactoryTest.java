package uk.nhs.adaptors.gp2gp.tasks;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class TaskExecutorFactoryTest {

    @Test
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask() {
        var alphaTaskExecutor = new AlphaTaskExecutor();
        var betaTaskExecutor = new BetaTaskExecutor();
        var executors = List.of(alphaTaskExecutor, betaTaskExecutor);
        TaskExecutorFactory taskExecutorFactory = new TaskExecutorFactory(executors);

        assertThat(taskExecutorFactory.getTaskExecutor(AlphaTaskDefinition.class)).isEqualTo(alphaTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(BetaTaskDefinition.class)).isEqualTo(betaTaskExecutor);
        assertThat(taskExecutorFactory.getTaskExecutor(NoExecutorTaskDefinition.class)).isNull();
    }

    private static class AlphaTaskDefinition extends TaskDefinition {

        AlphaTaskDefinition() {
            super("alpha", "alpha");
        }
    }

    private static class AlphaTaskExecutor implements TaskExecutor {

        @Override
        public Class<? extends TaskDefinition> getTaskType() {
            return AlphaTaskDefinition.class;
        }

        @Override
        public void execute(TaskDefinition taskDefinition) {

        }
    }

    private static class BetaTaskDefinition extends TaskDefinition {

        BetaTaskDefinition() {
            super("beta", "beta");
        }
    }

    private static class BetaTaskExecutor implements TaskExecutor {

        @Override
        public Class<? extends TaskDefinition> getTaskType() {
            return BetaTaskDefinition.class;
        }

        @Override
        public void execute(TaskDefinition taskDefinition) {

        }
    }

    private static class NoExecutorTaskDefinition extends TaskDefinition {

        NoExecutorTaskDefinition() {
            super("noExecutor", "noExecutor");
        }
    }
}
