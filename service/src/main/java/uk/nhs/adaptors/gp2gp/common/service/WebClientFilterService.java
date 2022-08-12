package uk.nhs.adaptors.gp2gp.common.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;

import static org.springframework.http.HttpStatus.*;

@Slf4j
public class WebClientFilterService {

    private static final String REQUEST_EXCEPTION_MESSAGE = "The following error occurred during %s request: %s";

    private static final Map<RequestType, Function<String, Exception>> REQUEST_TYPE_TO_EXCEPTION_MAP = Map.of(
        RequestType.GPC, GpConnectException::new);

    public enum RequestType {
        GPC, MHS_OUTBOUND
    }

    public static void addWebClientFilters(
        List<ExchangeFilterFunction> filters, RequestType requestType, HttpStatus expectedSuccessHttpStatus) {

        // filters are executed in reversed order
        filters.add(errorHandling(requestType, expectedSuccessHttpStatus));
        filters.add(logRequest());
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
            }

            return clientResponse.bodyToMono(String.class).flatMap(outcome -> {

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode outcomeJson = objectMapper.readTree(outcome);
                    var findValue = outcomeJson.findValues("code").stream();

                    boolean patientNotFound = findValue.anyMatch(code -> code.textValue().equals("PATIENT_NOT_FOUND"));
                    boolean notAuthorized = findValue.anyMatch(code -> code.textValue().equals("NOT_AUTHORISED"));
                    boolean invalidNhsNumber = findValue.anyMatch(code -> code.textValue().equals("INVALID_NHS_NUMBER"));
                    boolean invalidPatientDemographic = findValue.anyMatch(code -> code.textValue().equals("INVALID_PATIENT_DEMOGRAPHICS"));

                    var statusCode = clientResponse.statusCode();

                    if (statusCode.equals(NOT_FOUND) && patientNotFound) {
                        //error 6
                        return Mono.error(new GpConnectNotFoundException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                    } else if (statusCode.equals(UNAUTHORIZED) && notAuthorized) {
                        //error 19
                        return Mono.error(new GpConnectInvalidException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                    } else if (statusCode.equals(BAD_REQUEST) && invalidNhsNumber) {
                        //error 19
                        return Mono.error(new GpConnectInvalidException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                    } else if (statusCode.equals(BAD_REQUEST) && invalidPatientDemographic) {
                        //error 20
                        return Mono.error(new GpConnectException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));
                    }

                    return getResponseError(clientResponse, requestType);

                } catch (JsonProcessingException e) {
                    return Mono.error(new GpConnectException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));
                }
            });
        });
    }

    private static Mono<ClientResponse> getResponseError(ClientResponse clientResponse, RequestType requestType) {
        var exceptionBuilder = REQUEST_TYPE_TO_EXCEPTION_MAP
            .getOrDefault(requestType, InvalidOutboundMessageException::new);

        return clientResponse.bodyToMono(String.class)
            .flatMap(operationalOutcome -> Mono.error(
                exceptionBuilder.apply(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, operationalOutcome))
                )
            );
    }

    private static ExchangeFilterFunction logRequest() {
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

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (LOGGER.isDebugEnabled()) {
                var headers = response.headers().asHttpHeaders().entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(System.lineSeparator()));
                LOGGER.debug("Response: {} {} \n{}",
                    response.statusCode().value(), response.statusCode().getReasonPhrase(), headers);
            }

            return Mono.just(response);
        });
    }
}
