package uk.nhs.adaptors.gp2gp.common.service;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import uk.nhs.adaptors.gp2gp.common.configuration.WebClientConfiguration;
import uk.nhs.adaptors.gp2gp.common.exception.MaximumExternalAttachmentsException;
import uk.nhs.adaptors.gp2gp.common.exception.RetryLimitReachedException;
import uk.nhs.adaptors.gp2gp.gpc.exception.EhrRequestException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectInvalidException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectNotFoundException;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpcServerErrorException;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;

@Slf4j
public class WebClientFilterService {

    private static final String REQUEST_EXCEPTION_MESSAGE = "The following error occurred during %s request: %s";
    private static final String MAX_ATTACHMENTS_REGEX = ".*'external_attachments': \\[[^]]*Longer than maximum length[^]]*].*";
    private static final String PATIENT_NOT_FOUND_STATUS = "PATIENT_NOT_FOUND";
    private static final String NO_RELATIONSHIP_STATUS = "NO_RELATIONSHIP";
    private static final String INVALID_NHS_NUMBER_STATUS = "INVALID_NHS_NUMBER";
    private static final String INVALID_PATIENT_DEMOGRAPHICS_STATUS = "INVALID_PATIENT_DEMOGRAPHICS";
    private static final String INVALID_RESOURCE_STATUS = "INVALID_RESOURCE";
    private static final String BAD_REQUEST_STATUS = "BAD_REQUEST";
    private static final String INVALID_PARAMETER_STATUS = "INVALID_PARAMETER";
    private static final String INTERNAL_SERVER_ERROR_STATUS = "INTERNAL_SERVER_ERROR";

    private static final int NACK_ERROR_6 = 6;
    private static final int NACK_ERROR_18 = 18;
    private static final int NACK_ERROR_19 = 19;
    private static final int NACK_ERROR_20 = 20;

    private static final Map<RequestType, Function<String, Exception>> REQUEST_TYPE_TO_EXCEPTION_MAP = Map.of(
            RequestType.GPC, GpConnectException::new);

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
        TimeoutException.class,
        MhsServerErrorException.class,
        GpcServerErrorException.class
    );

    public enum RequestType {
        GPC, MHS_OUTBOUND
    }

    public static void addWebClientFilters(
            List<ExchangeFilterFunction> filters,
            RequestType requestType,
            HttpStatus expectedSuccessHttpStatus,
            WebClientConfiguration clientConfiguration) {

        // filters are executed in reversed order
        filters.add(retryPolicy(requestType, clientConfiguration));
        filters.add(errorHandling(requestType, expectedSuccessHttpStatus));
        filters.add(logRequest());
        filters.add(logResponse());
        filters.add(timeout(clientConfiguration));
        // this will be executed as the first one - always needs to come first
        filters.add(mdc());
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

            var statusCode = clientResponse.statusCode();
            if (statusCode.equals(httpStatus)) {
                LOGGER.info(requestType + " request successful status_code: {}", statusCode.value());
                return Mono.just(clientResponse);
            }
            if (statusCode.is5xxServerError()) {
                return getInternalServerErrorException(clientResponse, requestType);
            }
            if (requestType.equals(RequestType.GPC)) {
                return getErrorException(clientResponse, requestType);
            }
            if (requestType.equals(RequestType.MHS_OUTBOUND) && statusCode.value() == BAD_REQUEST.value()) {

                return clientResponse
                        .bodyToMono(String.class)
                        .flatMap(WebClientFilterService::handle400FromMhsOutbound);
            }

            return getResponseError(clientResponse, requestType);
        });
    }

    private static Mono<ClientResponse> handle400FromMhsOutbound(String body) {

        Pattern pattern = Pattern.compile(MAX_ATTACHMENTS_REGEX, CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);

        if (matcher.find()) {
            return Mono.error(
                    new MaximumExternalAttachmentsException(String.format(REQUEST_EXCEPTION_MESSAGE, RequestType.MHS_OUTBOUND, body))
            );
        }

        return Mono.error(new InvalidOutboundMessageException(String.format(REQUEST_EXCEPTION_MESSAGE, RequestType.MHS_OUTBOUND, body)));
    }

    private static Mono<ClientResponse> getErrorException(ClientResponse clientResponse, RequestType requestType) {

        return clientResponse.bodyToMono(String.class).flatMap(outcome -> {
            var exceptionMessage = String.format(REQUEST_EXCEPTION_MESSAGE, requestType, outcome);

            try {
                var objectMapper = new ObjectMapper();
                var outcomeJson = objectMapper.readTree(outcome);
                var codes = outcomeJson.findValuesAsText("code");
                var statusCode = clientResponse.statusCode();
                var errorCode = getErrorCode(statusCode, codes);

                return getMonoError(errorCode, exceptionMessage);
            } catch (JsonProcessingException e) {
                return Mono.error(new GpConnectException(exceptionMessage));
            }
        });
    }

    private static int getErrorCode(HttpStatus statusCode, List<String> codes) {
        switch (statusCode) {
            case NOT_FOUND:
                if (codes.contains(PATIENT_NOT_FOUND_STATUS)) {
                    return NACK_ERROR_6;
                }
                break;
            case FORBIDDEN:
                if (codes.contains(NO_RELATIONSHIP_STATUS)) {
                    return NACK_ERROR_19;
                }
                break;
            case BAD_REQUEST:
                if (codes.contains(INVALID_NHS_NUMBER_STATUS)) {
                    return NACK_ERROR_19;
                } else if (codes.contains(INVALID_PATIENT_DEMOGRAPHICS_STATUS)) {
                    return NACK_ERROR_20;
                } else if (codes.contains(BAD_REQUEST_STATUS)) {
                    return NACK_ERROR_18;
                }
                break;
            case UNPROCESSABLE_ENTITY:
                if (codes.contains(INVALID_RESOURCE_STATUS)
                        || codes.contains(BAD_REQUEST_STATUS)
                        || codes.contains(INVALID_PARAMETER_STATUS)) {
                    return NACK_ERROR_18;
                }
                break;
            default:
                return NACK_ERROR_20;
        }
        return NACK_ERROR_20;
    }

    private static Mono<ClientResponse> getMonoError(int errorCode, String exceptionMessage) {
        switch (errorCode) {
            case NACK_ERROR_6:
                return Mono.error(new GpConnectNotFoundException(exceptionMessage));
            case NACK_ERROR_19:
                return Mono.error(new GpConnectInvalidException(exceptionMessage));
            case NACK_ERROR_18:
                return Mono.error(new EhrRequestException(exceptionMessage));
            case NACK_ERROR_20:
            default:
                return Mono.error(new GpConnectException(exceptionMessage));
        }
    }

    private static Mono<ClientResponse> getResponseError(ClientResponse clientResponse, RequestType requestType) {
        var exceptionBuilder = REQUEST_TYPE_TO_EXCEPTION_MAP
                .getOrDefault(requestType, InvalidOutboundMessageException::new);

        return clientResponse
                .bodyToMono(String.class)
                .flatMap(operationalOutcome -> {
                    var exceptionMessage = String.format(REQUEST_EXCEPTION_MESSAGE, requestType, operationalOutcome);

                    return Mono.error(exceptionBuilder.apply(exceptionMessage));
                });
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

    private static Mono<ClientResponse> getInternalServerErrorException(ClientResponse clientResponse, RequestType requestType) {

        if (requestType.equals(RequestType.MHS_OUTBOUND)) {
            return Mono.error(new MhsServerErrorException(
                String.format(REQUEST_EXCEPTION_MESSAGE, requestType, clientResponse.statusCode()))
            );
        }

        return clientResponse.toEntity(String.class).flatMap(response -> {
            String exceptionMessage;

            if (response.hasBody()) {
                exceptionMessage = String.format(REQUEST_EXCEPTION_MESSAGE, requestType, response.getBody());
            } else {
                exceptionMessage = String.format(REQUEST_EXCEPTION_MESSAGE, requestType, clientResponse.statusCode());
            }

            return Mono.error(new GpcServerErrorException(exceptionMessage));
        });
    }

    private static ExchangeFilterFunction retryPolicy(RequestType requestType, WebClientConfiguration clientConfiguration) {
        return (request, next) ->
            next.exchange(request)
                .retryWhen(getRetryPolicyByRequestType(requestType, clientConfiguration));
    }

    private static ExchangeFilterFunction timeout(WebClientConfiguration clientConfiguration) {
        return (request, next) -> next.exchange(request).timeout(clientConfiguration.getTimeout());
    }

    private static Retry getRetryPolicyByRequestType(RequestType type, WebClientConfiguration clientConfiguration) {
        return Retry
            .backoff(clientConfiguration.getMaxBackoffAttempts(), clientConfiguration.getMinBackOff())
            .filter(exception -> RETRYABLE_EXCEPTIONS.contains(exception.getClass()))
            .doBeforeRetry(retrySignal ->
                LOGGER.info("Request to `{}` failed, retrying request {}/{}",
                    type, retrySignal.totalRetries() + 1, clientConfiguration.getMaxBackoffAttempts()))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                new RetryLimitReachedException(String.format("Retries exhausted: %s/%s",
                        retrySignal.totalRetries(), clientConfiguration.getMaxBackoffAttempts()), retrySignal.failure()
                )
            );

    }
}
