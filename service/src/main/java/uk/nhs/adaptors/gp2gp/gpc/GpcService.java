package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;

@Service
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcService {

    private final GpcClient gpcClient;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractRequestHandler ehrExtractRequestHandler;

    public void handleStructureTask(GetGpcStructuredTaskDefinition structuredTaskDefinition) throws IOException {
        var ehrExtractStatus = ehrExtractRequestHandler.getEhrExtractStatus(structuredTaskDefinition);

        var requestBodyParameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBodyParameters, ehrExtractStatus);
        var response = gpcClient.getStructuredRecord(request, structuredTaskDefinition, ehrExtractStatus);

        storageConnectorService.handleStructuredRecord(response);
        ehrExtractRequestHandler.updateEhrExtractStatusAccessStructured(structuredTaskDefinition, ehrExtractStatus);
    }
}
