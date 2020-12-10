package uk.nhs.adaptors.gp2gp.tasks;

public class GetGpcStructuredTaskExecutor implements TaskExecutor{
    @Override
    public Class<? extends TaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(TaskDefinition taskDefinition) {
        GetGpcStructuredTaskDefinition gpcStructuredTaskDefinition = (GetGpcStructuredTaskDefinition) taskDefinition;
    }
}
