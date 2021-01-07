package uk.nhs.adaptors.gp2gp.common.task;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TaskIdServiceTest {

    @Test
    public void When_CreateTaskId_Expect_TaskIdIsRandomUUID() {
        String taskId1 = new TaskIdService().createNewTaskId();
        String taskId2 = new TaskIdService().createNewTaskId();
        assertThatCode(() -> UUID.fromString(taskId1))
            .doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(taskId2))
            .doesNotThrowAnyException();
        assertThat(taskId1).isNotEqualTo(taskId2);
    }

}