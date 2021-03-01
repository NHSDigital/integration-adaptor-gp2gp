package uk.nhs.adaptors.gp2gp.common.service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import uk.nhs.adaptors.gp2gp.sds.exception.SdsException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;

@Slf4j
@Component
@Service
public class WebClientFilterService {

    private static final Map<RequestType, Function<String, Exception>> REQUEST_TYPE_TO_EXCEPTION_MAP = Map.of(
        RequestType.GPC, GpConnectException::new,
        RequestType.SDS, SdsException::new);

    public enum RequestType {
        GPC, SDS, MHS_OUTBOUND
    }

    public ExchangeFilterFunction errorHandlingFilter(RequestType requestType, HttpStatus httpStatus) {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(httpStatus)) {
                LOGGER.info(requestType + " request successful, status code: {}", clientResponse.statusCode());
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

    private Mono<ClientResponse> getResponseError(ClientResponse clientResponse, RequestType requestType) {
        var exceptionBuilder = REQUEST_TYPE_TO_EXCEPTION_MAP
            .getOrDefault(requestType, InvalidOutboundMessageException::new);

        return clientResponse.bodyToMono(String.class)
            .flatMap(operationalOutcome -> Mono.error(
                exceptionBuilder.apply(
                    "The following error occurred during " + requestType + " request: " + operationalOutcome)));
    }
}
