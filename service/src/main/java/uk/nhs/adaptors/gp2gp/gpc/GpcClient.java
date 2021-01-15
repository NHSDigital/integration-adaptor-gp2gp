package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private final GpcConfiguration gpcConfiguration;

    public GpcStructuredResponseObject getStructuredRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Gpc Structured Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            structuredTaskDefinition.getToAsid(),
            structuredTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getStructuredEndpoint());

        var responseString = request
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return new GpcStructuredResponseObject(
            structuredTaskDefinition.getTaskType().getTaskTypeHeaderValue(),
            structuredTaskDefinition.getConversationId(),
            structuredTaskDefinition.getTaskId(),
            responseString
        );
    }

    public GpcDocumentResponseObject getDocumentRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
            GetGpcDocumentTaskDefinition documentTaskDefinition) {
        LOGGER.info("Gpc Document Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            documentTaskDefinition.getToAsid(),
            documentTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getDocumentEndpoint());

        var responseString = request
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return new GpcDocumentResponseObject(
            documentTaskDefinition.getTaskType().getTaskTypeHeaderValue(),
            documentTaskDefinition.getConversationId(),
            documentTaskDefinition.getTaskId(),
            responseString
        );
    }
}

