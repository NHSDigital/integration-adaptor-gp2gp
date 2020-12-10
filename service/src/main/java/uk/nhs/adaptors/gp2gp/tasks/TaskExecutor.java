package uk.nhs.adaptors.gp2gp.tasks;

public interface TaskExecutor {

    Class<? extends TaskDefinition> getTaskType();

    void execute(TaskDefinition taskDefinition);
}
