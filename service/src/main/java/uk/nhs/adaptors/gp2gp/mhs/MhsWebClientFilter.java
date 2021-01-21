package uk.nhs.adaptors.gp2gp.mhs;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MhsWebClientFilter {
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(HttpStatus.ACCEPTED)) {
                LOGGER.info("Mhs Outbound Request successful, status code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            } else {
                return getResponseError(clientResponse);
            }
        });
    }

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
            .flatMap(operationalOutcome -> Mono.error(
                new InvalidOutboundMessageException("The following error occurred during Mhs Outbound Request: " + operationalOutcome)));
    }
}
