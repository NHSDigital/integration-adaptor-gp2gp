package uk.nhs.adaptors.gp2gp.ehr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrExtractCoreTaskExecutor implements TaskExecutor<SendEhrExtractCoreTaskDefinition> {
    @Override
    public Class<SendEhrExtractCoreTaskDefinition> getTaskType() {
        return SendEhrExtractCoreTaskDefinition.class;
    }

    @Override
    public void execute(SendEhrExtractCoreTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from SendEhrExtractCoreTaskExecutor");
    }
}
