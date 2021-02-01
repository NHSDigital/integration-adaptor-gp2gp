package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private static final String STRUCTURED_LOG_TEMPLATE = "Gpc Access Structured Request, toASID: {}, fromASID: {}, Gpc Url: {}";
    private static final String DOCUMENT_LOG_TEMPLATE = "Gpc Access Document Request, toASID: {}, fromASID: {}, Gpc Url: {}";

    private final GpcConfiguration gpcConfiguration;

    public String getStructuredRecord(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request,
            GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        logRequest(STRUCTURED_LOG_TEMPLATE, structuredTaskDefinition, gpcConfiguration.getUrl() + gpcConfiguration.getStructuredEndpoint());

        return performRequestWithStringResponseBody(request);
    }

    public String getDocumentRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
            GetGpcDocumentTaskDefinition documentTaskDefinition) {
        logRequest(DOCUMENT_LOG_TEMPLATE, documentTaskDefinition, gpcConfiguration.getUrl() + gpcConfiguration.getDocumentEndpoint());

        return performRequestWithStringResponseBody(request);
    }

    private void logRequest(String logTemplate, TaskDefinition taskDefinition, String url) {
        LOGGER.info(logTemplate,
            taskDefinition.getToAsid(),
            taskDefinition.getFromAsid(),
            url);
    }

    private String performRequestWithStringResponseBody(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        return request.retrieve()
            .bodyToMono(String.class)
            .block();
    }
}

