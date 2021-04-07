package uk.nhs.adaptors.gp2gp.mhs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsClient {
    private final MhsConfiguration mhsConfiguration;

    public String sendMessageToMHS(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {
        LOGGER.info("Mhs Outbound Request, Mhs Outbound Endpoint: {}", mhsConfiguration.getUrl());

        return request
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
