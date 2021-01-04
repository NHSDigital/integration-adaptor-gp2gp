package uk.nhs.adaptors.gp2gp.gpc;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
public class GetGpcDocumentTaskExecutor implements TaskExecutor {

    @Override
    public Class<? extends TaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    public void execute(TaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");
    }
}
