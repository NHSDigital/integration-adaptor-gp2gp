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
    private static final int MAX_LOG_LINE_LENGTH = 100_000;

    private final GpcConfiguration gpcConfiguration;
    private final GpcRequestBuilder gpcRequestBuilder;

    public String getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        String gpcBaseUrlWithOds = buildGpcBaseUrl(structuredTaskDefinition);
        var requestBody = gpcRequestBuilder.buildGetStructuredRecordRequestBody(structuredTaskDefinition);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBody, structuredTaskDefinition, gpcBaseUrlWithOds);

        logRequest(STRUCTURED_LOG_TEMPLATE, structuredTaskDefinition,
            gpcConfiguration.getUrl() + gpcConfiguration.getMigrateStructuredEndpoint());

        return performRequest(request);
    }

    public String getDocumentRecord(GetGpcDocumentTaskDefinition documentReferencesTaskDefinition) {
        String gpcBaseUrlWithOds = buildGpcBaseUrl(documentReferencesTaskDefinition);
        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(documentReferencesTaskDefinition, gpcBaseUrlWithOds);

        logRequest(DOCUMENT_LOG_TEMPLATE, documentReferencesTaskDefinition, gpcBaseUrlWithOds);

        return performRequest(request);
    }

    private void logRequest(String logTemplate, TaskDefinition taskDefinition, String url) {
        LOGGER.debug(logTemplate,
            taskDefinition.getToAsid(),
            taskDefinition.getFromAsid(),
            url);
    }

    private String performRequest(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        var response = request.retrieve();
        var responseBody = response.bodyToMono(String.class).block();

        LOGGER.debug("Body: {}", responseBody);
        if (responseBody != null) {
            LOGGER.debug("Short body: {}", responseBody.substring(0, Math.min(responseBody.length(), MAX_LOG_LINE_LENGTH)));
        }

        return responseBody;
    }

    private String buildGpcBaseUrl(TaskDefinition taskDefinition) {
        return gpcConfiguration.getUrl().replace(ODS_CODE_PLACEHOLDER, taskDefinition.getToOdsCode());
    }
}

