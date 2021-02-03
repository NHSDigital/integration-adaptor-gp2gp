package uk.nhs.adaptors.gp2gp.common.service;

import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Slf4j
@Component
@Service
public class WebClientFilterService {
    public ExchangeFilterFunction errorHandlingFilter(String requestType, HttpStatus httpStatus) {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(httpStatus)) {
                LOGGER.info(requestType + " Request successful, status code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            } else {
                return getResponseError(clientResponse, requestType);
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

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse, String requestType) {
        if (requestType.equals("Gpc")) {
            return clientResponse.bodyToMono(String.class)
                .flatMap(operationalOutcome -> Mono.error(
                    new GpConnectException("The following error occurred during "
                        + requestType + " Request: " + operationalOutcome)));
        } else {
            return clientResponse.bodyToMono(String.class)
                .flatMap(operationalOutcome -> Mono.error(
                    new InvalidOutboundMessageException("The following error occurred during "
                        + requestType + " Request: " + operationalOutcome)));
        }
    }
}
