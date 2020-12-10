package uk.nhs.adaptors.gp2gp.tasks;

public class GetGpcDocumentTaskExecutor implements TaskExecutor {
    @Override
    public Class<? extends TaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    public void execute(TaskDefinition taskDefinition) {
        GetGpcDocumentTaskDefinition getGpcDocumentTaskDefinition = (GetGpcDocumentTaskDefinition) taskDefinition;
    }
}
