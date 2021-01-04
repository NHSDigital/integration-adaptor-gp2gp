package uk.nhs.adaptors.gp2gp.common.task;

public interface TaskExecutor {

    Class<? extends TaskDefinition> getTaskType();

    void execute(TaskDefinition taskDefinition);
}
