package uk.nhs.adaptors.gp2gp.common.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class WebClientFilterService {

    private static final Map<RequestType, Function<String, Exception>> REQUEST_TYPE_TO_EXCEPTION_MAP = Map.of(
        RequestType.GPC, GpConnectException::new);

    public enum RequestType {
        GPC, MHS_OUTBOUND
    }

    public static void addWebClientFilters(
        List<ExchangeFilterFunction> filters, RequestType requestType, HttpStatus expectedSuccessHttpStatus) {

        // filters are executed in reversed order
        filters.add(errorHandling(requestType, expectedSuccessHttpStatus));
        filters.add(logResponse());
        filters.add(mdc()); // this will be executed as the first one - always needs to come first
    }

    private static ExchangeFilterFunction mdc() {
        var mdc = MDC.getCopyOfContextMap();
        return (request, next) -> next.exchange(request)
            .doOnNext(value -> {
                if (mdc != null) {
                    MDC.setContextMap(mdc);
                }
            });
    }

    private static ExchangeFilterFunction errorHandling(RequestType requestType, HttpStatus httpStatus) {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            clientResponse.statusCode();
            if (clientResponse.statusCode().equals(httpStatus)) {
                LOGGER.info(requestType + " request successful status_code: {}", clientResponse.statusCode());
                return Mono.just(clientResponse);
            } else {
                return getResponseError(clientResponse, requestType);
            }
        });
    }

    private static Mono<ClientResponse> getResponseError(ClientResponse clientResponse, RequestType requestType) {
        var exceptionBuilder = REQUEST_TYPE_TO_EXCEPTION_MAP
            .getOrDefault(requestType, InvalidOutboundMessageException::new);

        return clientResponse.bodyToMono(String.class)
            .flatMap(operationalOutcome -> Mono.error(
                exceptionBuilder.apply(
                    "The following error occurred during " + requestType + " request: " + operationalOutcome)));
    }

    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            logStatus(response);
            logHeaders(response);

            return logResponseBody(response);
        });
    }

    private static void logStatus(ClientResponse response) {
        HttpStatus status = response.statusCode();
        LOGGER.debug("Response: {} ({})", status.value(), status.getReasonPhrase());
    }

    private static Mono<ClientResponse> logResponseBody(ClientResponse response) {
        response.statusCode();
        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
            return response.bodyToMono(String.class)
                .flatMap(body -> {
                    LOGGER.debug("Body: {}", body);
                    return Mono.just(response);
                });
        } else {
            return Mono.just(response);
        }
    }

    private static void logHeaders(ClientResponse response) {
        response.headers().asHttpHeaders().forEach((name, values) -> {
            values.forEach(value -> logNameAndValuePair(name, value));
        });
    }

    private static void logNameAndValuePair(String name, String value) {
        LOGGER.debug("Header: {}={}", name, value);
    }
}
