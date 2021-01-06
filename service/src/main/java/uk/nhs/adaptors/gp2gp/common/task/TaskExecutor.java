package uk.nhs.adaptors.gp2gp.common.task;

public interface TaskExecutor<T extends TaskDefinition> {

    Class<T> getTaskType();

    void execute(T taskDefinition);
}
