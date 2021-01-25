package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrExtractCoreTaskExecutor implements TaskExecutor<SendEhrExtractCoreTaskDefinition> {
    private final MhsClient mhsClient;
    private final MhsRequestBuilder mhsRequestBuilder;
    private EhrExtractStatusService ehrExtractStatusService;

    @Override
    public Class<SendEhrExtractCoreTaskDefinition> getTaskType() {
        return SendEhrExtractCoreTaskDefinition.class;
    }

    @Override
    public void execute(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition) {
        LOGGER.info("Execute called from SendEhrExtractCoreTaskExecutor");

        Instant requestSentAt = Instant.now();
        var request = mhsRequestBuilder.buildSendEhrExtractCoreRequest(sendEhrExtractCoreTaskDefinition);
        var response = mhsClient.sendEhrExtractCore(request, sendEhrExtractCoreTaskDefinition);

        // validation on != 202 here?
        ehrExtractStatusService.updateEhrExtractStatusCore(sendEhrExtractCoreTaskDefinition, requestSentAt);
    }
}
