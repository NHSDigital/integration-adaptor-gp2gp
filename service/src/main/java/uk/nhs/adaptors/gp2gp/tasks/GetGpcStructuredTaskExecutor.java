package uk.nhs.adaptors.gp2gp.tasks;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetGpcStructuredTaskExecutor implements TaskExecutor{
    @Override
    public Class<? extends TaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(TaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");
    }
}
