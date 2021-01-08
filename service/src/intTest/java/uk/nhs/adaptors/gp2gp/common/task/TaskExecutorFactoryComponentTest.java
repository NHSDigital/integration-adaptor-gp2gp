package uk.nhs.adaptors.gp2gp.common.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskExecutor;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({ SpringExtension.class, MongoDBExtension.class})
@SpringBootTest
public class TaskExecutorFactoryComponentTest {
    @Autowired
    private TaskExecutorFactory taskExecutorFactory;

    @Test
    public void When_TaskExecutorFactoryBeanCreated_Expect_ExecutorsInjected() {
        assertThat(taskExecutorFactory.getTaskExecutor(GetGpcStructuredTaskDefinition.class))
            .isNotNull()
            .isInstanceOf(GetGpcStructuredTaskExecutor.class);
    }
}
