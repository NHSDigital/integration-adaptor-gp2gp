package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;

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
            gpcConfiguration.getUrl() + gpcConfiguration.getEndpoint());

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
}

