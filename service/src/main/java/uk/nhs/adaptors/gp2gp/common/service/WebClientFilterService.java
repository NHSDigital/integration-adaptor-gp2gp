package uk.nhs.adaptors.gp2gp.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.GpConnectException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;

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
