package uk.nhs.adaptors.gp2gp.gpc;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcStructuredTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");
    }
}
