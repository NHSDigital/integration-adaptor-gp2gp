package uk.nhs.adaptors.gp2gp.mhs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsClient {
    private final MhsConfiguration mhsConfiguration;

    public String sendEhrExtractCore(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request,
                                      SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition) {
        LOGGER.info("Mhs Outbound Request, Mhs Outbound Endpoint: {}", mhsConfiguration.getUrl());

        return request
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
