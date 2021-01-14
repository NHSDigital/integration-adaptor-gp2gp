package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GpcWebClientFilter {
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().is5xxServerError()
                || clientResponse.statusCode().is4xxClientError()) {
                return getResponseError(clientResponse);
            } else {
                LOGGER.info("Gpc Structured Request successful, status code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            }
        });
    }

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
            .flatMap(errorBody -> Mono.error(new GpcStructuredAccessException(errorBody)));
    }
}
