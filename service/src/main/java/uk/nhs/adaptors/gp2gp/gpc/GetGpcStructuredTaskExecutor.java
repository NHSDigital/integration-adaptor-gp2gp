package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    private final GpcClient gpcClient;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final StorageConnectorService storageConnectorService;
    private final GpcPatientDataHandler gpcPatientHandler;
    private final TaskDispatcher taskDispatcher;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) throws IOException {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        var requestBodyParameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBodyParameters, structuredTaskDefinition);
        var response = gpcClient.getStructuredRecord(request, structuredTaskDefinition);

        storageConnectorService.handleStructuredRecord(response);
        gpcPatientHandler.updateEhrExtractStatusAccessStructured(structuredTaskDefinition);

        var getGpcDocumentTaskDefinition = buildDocumentTask(response, structuredTaskDefinition);
        taskDispatcher.createTask(getGpcDocumentTaskDefinition);
    }

    private GetGpcDocumentTaskDefinition buildDocumentTask(GpcStructuredResponseObject response, GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(response.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION)
            .taskId(structuredTaskDefinition.getTaskId())
            .conversationId(structuredTaskDefinition.getConversationId())
            .requestId(structuredTaskDefinition.getRequestId())
            .fromAsid(structuredTaskDefinition.getFromAsid())
            .toAsid(structuredTaskDefinition.getToAsid())
            .fromOdsCode(structuredTaskDefinition.getFromOdsCode()).build();
    }
}
