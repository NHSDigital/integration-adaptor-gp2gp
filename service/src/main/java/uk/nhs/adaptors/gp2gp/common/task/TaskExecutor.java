package uk.nhs.adaptors.gp2gp.common.task;

import java.io.IOException;

public interface TaskExecutor<T extends TaskDefinition> {

    Class<T> getTaskType();

    void execute(T taskDefinition) throws IOException;
}
