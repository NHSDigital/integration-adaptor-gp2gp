package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    private final GpcConfiguration gpcConfiguration;

    public GpcStructuredResponseObject getStructuredRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
        GetGpcStructuredTaskDefinition structuredTaskDefinition, EhrExtractStatus ehrExtractStatus) {
        LOGGER.info("Gpc Structured Request, toASID: {}, fromASID: {}, Gpc Endpoint: {}",
            ehrExtractStatus.getEhrRequest().getToAsid(),
            ehrExtractStatus.getEhrRequest().getFromAsid(),
            gpcConfiguration.getUrl() + gpcConfiguration.getEndpoint());

        var responseString =  request
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
}

