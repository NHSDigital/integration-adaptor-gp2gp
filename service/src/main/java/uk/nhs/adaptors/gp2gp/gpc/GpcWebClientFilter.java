package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

@Slf4j
@Component
public class GpcWebClientFilter {
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(HttpStatus.OK)) {
                LOGGER.info("Gpc request successful, status code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            } else {
                return getResponseError(clientResponse);
            }
        });
    }

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
            .flatMap(operationalOutcome -> Mono.error(
                new GpConnectException("The following error occurred during Gpc Request: " + operationalOutcome)));
    }
}
