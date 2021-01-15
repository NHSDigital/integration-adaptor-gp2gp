package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private final GpcConfiguration gpcConfiguration;

    public StorageDataWrapper getStructuredRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Gpc Structured Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            structuredTaskDefinition.getToAsid(),
            structuredTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getStructuredEndpoint());

        var responseString = request
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return new StorageDataWrapper(
            structuredTaskDefinition.getTaskType().getTaskTypeHeaderValue(),
            structuredTaskDefinition.getConversationId(),
            structuredTaskDefinition.getTaskId(),
            responseString
        );
    }

    public StorageDataWrapper getDocumentRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
            GetGpcDocumentTaskDefinition documentTaskDefinition) {
        LOGGER.info("Gpc Document Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            documentTaskDefinition.getToAsid(),
            documentTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getDocumentEndpoint());

        var responseString = request
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return new StorageDataWrapper(
            documentTaskDefinition.getTaskType().getTaskTypeHeaderValue(),
            documentTaskDefinition.getConversationId(),
            documentTaskDefinition.getTaskId(),
            responseString
        );
    }
}

