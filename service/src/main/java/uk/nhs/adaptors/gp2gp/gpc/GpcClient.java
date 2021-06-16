package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private static final String ODS_CODE_PLACEHOLDER = "@ODS_CODE@";
    private static final String STRUCTURED_LOG_TEMPLATE = "Gpc Access Structured Request, toASID: {}, fromASID: {}, Gpc Url: {}";
    private static final String DOCUMENT_LOG_TEMPLATE = "Gpc Access Document Request, toASID: {}, fromASID: {}, Gpc Url: {}";
    private static final String PATIENT_LOG_TEMPLATE = "Gpc Access Patient Request, toASID: {}, fromASID: {}, Gpc Url: {}";
    private static final String PATIENT_DOCUMENTS_LOG_TEMPLATE = "Gpc Access Patient Documents, toASID: {}, fromASID: {}, Gpc Url: {}";

    private final GpcConfiguration gpcConfiguration;
    private final GpcRequestBuilder gpcRequestBuilder;

    public String getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        var requestBodyParameters = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
        String gpcBaseUrl = buildGpcBaseUrl(structuredTaskDefinition);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBodyParameters, structuredTaskDefinition, gpcBaseUrl);

        logRequest(STRUCTURED_LOG_TEMPLATE, structuredTaskDefinition, gpcBaseUrl + gpcConfiguration.getStructuredEndpoint());

        return performRequest(request);
    }

    public String getDocumentRecord(GetGpcDocumentTaskDefinition documentTaskDefinition) {
        String gpcBaseUrl = buildGpcBaseUrl(documentTaskDefinition);
        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(documentTaskDefinition, gpcBaseUrl);

        logRequest(DOCUMENT_LOG_TEMPLATE, documentTaskDefinition, gpcBaseUrl + gpcConfiguration.getDocumentEndpoint());

        return performRequest(request);
    }

    public String getPatientRecord(GetGpcDocumentReferencesTaskDefinition patientIdentifierTaskDefinition) {
        String gpcBaseUrl = buildGpcBaseUrl(patientIdentifierTaskDefinition);
        var request = gpcRequestBuilder.buildGetPatientIdentifierRequest(patientIdentifierTaskDefinition, gpcBaseUrl);

        logRequest(PATIENT_LOG_TEMPLATE, patientIdentifierTaskDefinition, gpcBaseUrl + gpcConfiguration.getDocumentEndpoint());

        return performRequest(request);
    }

    public String getDocumentReferences(GetGpcDocumentReferencesTaskDefinition documentReferencesTaskDefinition, String patientId) {
        String gpcBaseUrl = buildGpcBaseUrl(documentReferencesTaskDefinition);
        var request = gpcRequestBuilder.buildGetPatientDocumentReferences(documentReferencesTaskDefinition, patientId, gpcBaseUrl);

        logRequest(PATIENT_DOCUMENTS_LOG_TEMPLATE, documentReferencesTaskDefinition, gpcBaseUrl + gpcConfiguration.getPatientEndpoint());

        return performRequest(request);
    }

    private void logRequest(String logTemplate, TaskDefinition taskDefinition, String url) {
        LOGGER.info(logTemplate,
            taskDefinition.getToAsid(),
            taskDefinition.getFromAsid(),
            url);
    }

    private String performRequest(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        return request.retrieve()
            .bodyToMono(String.class)
            .block();
    }

    private String buildGpcBaseUrl(TaskDefinition taskDefinition) {
        return gpcConfiguration.getUrl().replace(ODS_CODE_PLACEHOLDER, taskDefinition.getToOdsCode());
    }
}

