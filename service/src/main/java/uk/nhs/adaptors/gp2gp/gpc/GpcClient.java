package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private final GpcConfiguration gpcConfiguration;

    public String getStructuredRecord(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Gpc Structured Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            structuredTaskDefinition.getToAsid(),
            structuredTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getStructuredEndpoint());

        return request.retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public String getDocumentRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
            GetGpcDocumentTaskDefinition documentTaskDefinition) {
        LOGGER.info("Gpc Document Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            documentTaskDefinition.getToAsid(),
            documentTaskDefinition.getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getDocumentEndpoint());

        return request.retrieve()
            .bodyToMono(String.class)
            .block();
    }
}

