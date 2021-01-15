package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {
    private final GpcClient gpcClient;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final StorageConnectorService storageConnectorService;
    private final GpcPatientDataHandler gpcPatientHandler;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        var requestBodyParameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBodyParameters, structuredTaskDefinition);
        var response = gpcClient.getStructuredRecord(request, structuredTaskDefinition);

        storageConnectorService.uploadWithMetadata(response);
        gpcPatientHandler.updateEhrExtractStatusAccessStructured(structuredTaskDefinition);
    }
}
