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

import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

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
            if (requestType.equals(RequestType.GPC)) {
                return getErrorException(clientResponse, requestType);
            }
            if (requestType.equals(RequestType.MHS_OUTBOUND) && httpStatus.is5xxServerError()) {
                return Mono.error(new MhsServerErrorException("MHS responded with status code " + httpStatus.value()));
            }

            return getResponseError(clientResponse, requestType);
        });
    }

    private static Mono<ClientResponse> getErrorException(ClientResponse clientResponse, RequestType requestType) {

        return clientResponse.bodyToMono(String.class).flatMap(outcome -> {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode outcomeJson = objectMapper.readTree(outcome);
                var findValue = outcomeJson.findValues("code");

                boolean patientNotFound = findValue.stream().anyMatch(code -> code.textValue().equals("PATIENT_NOT_FOUND"));
                boolean notAuthorized = findValue.stream().anyMatch(code -> code.textValue().equals("NOT_AUTHORISED"));
                boolean invalidNhsNumber = findValue.stream().anyMatch(code -> code.textValue().equals("INVALID_NHS_NUMBER"));
                boolean invalidPatientDemographic = findValue.stream().anyMatch(
                        code -> code.textValue().equals("INVALID_PATIENT_DEMOGRAPHICS"));
                boolean invalidResource = findValue.stream().anyMatch(code -> code.textValue().equals("INVALID_RESOURCE"));
                boolean badRequest = findValue.stream().anyMatch(code -> code.textValue().equals("BAD_REQUEST"));
                boolean invalidParameter = findValue.stream().anyMatch(code -> code.textValue().equals("INVALID_PARAMETER"));
                boolean internalServErr = findValue.stream().anyMatch(code -> code.textValue().equals("INTERNAL_SERVER_ERROR"));

                var statusCode = clientResponse.statusCode();

                if (statusCode.equals(NOT_FOUND) && patientNotFound) {
                    //error 6
                    return Mono.error(new GpConnectNotFoundException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                } else if (statusCode.equals(UNAUTHORIZED) && notAuthorized || statusCode.equals(BAD_REQUEST) && invalidNhsNumber) {
                    //error 19
                    return Mono.error(new GpConnectInvalidException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                } else if (statusCode.equals(BAD_REQUEST) && invalidPatientDemographic
                        || (statusCode.equals(INTERNAL_SERVER_ERROR) && internalServErr)) {
                    //error 20
                    return Mono.error(new GpConnectException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

                } else if (statusCode.equals(UNPROCESSABLE_ENTITY) && invalidResource
                        || statusCode.equals(BAD_REQUEST) && badRequest
                        || statusCode.equals(UNPROCESSABLE_ENTITY) && invalidParameter) {
                    //error 18
                    return Mono.error(new EhrRequestException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));
                }
                //default error 20
                return Mono.error(new GpConnectException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));

            } catch (JsonProcessingException e) {
                return Mono.error(new GpConnectException(String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome)));
            }
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
