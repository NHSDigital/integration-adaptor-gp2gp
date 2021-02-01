package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Component
public class GpcWebClientFilter {
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(HttpStatus.OK)) {
                LOGGER.info("Gpc Request successful, status code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            } else {
                return getResponseError(clientResponse);
            }
        });
    }

    public ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            if (LOGGER.isDebugEnabled()) {
                var headers = clientRequest.headers().entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(System.lineSeparator()));
                LOGGER.debug("Request: {} {} \n{}", clientRequest.method(), clientRequest.url(), headers);
            }
            return next.exchange(clientRequest);
        };
    }

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
            .flatMap(response -> Mono.error(
                new GpConnectException("The following error occurred during Gpc Request: " + response)));
    }
}
