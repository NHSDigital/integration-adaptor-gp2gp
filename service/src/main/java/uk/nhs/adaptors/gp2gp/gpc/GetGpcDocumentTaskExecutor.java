package uk.nhs.adaptors.gp2gp.gpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@Component
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");
    }
}
