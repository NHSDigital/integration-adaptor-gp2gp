package uk.nhs.adaptors.gp2gp.sds.builder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.common.service.RequestBuilderService;
import uk.nhs.adaptors.gp2gp.common.service.WebClientFilterService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.sds.configuration.SdsConfiguration;

import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SdsRequestBuilder {
    private static final String PIPE_ENCODED = "%7C";
    private static final String ORG_CODE_PARAMETER = "organization";
    private static final String ORG_CODE_IDENTIFIER = "https://fhir.nhs.uk/Id/ods-organization-code";
    private static final String INTERACTION_PARAMETER = "identifier";
    private static final String INTERACTION_IDENTIFIER = "https://fhir.nhs.uk/Id/nhsServiceInteractionId";
    private static final String ENDPOINT = "/Endpoint";

    private static final String GET_STRUCTURED_INTERACTION =
        "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    private static final String SEARCH_FOR_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    private static final String RETRIEVE_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";

    private static final String API_KEY_HEADER = "apikey";
    private static final String X_CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final SdsConfiguration sdsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    public WebClient.RequestHeadersSpec<?> buildGetStructuredRecordRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), GET_STRUCTURED_INTERACTION, task.getConversationId());
    }

    public WebClient.RequestHeadersSpec<?> buildPatientSearchAccessDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION, task.getConversationId());
    }

    public WebClient.RequestHeadersSpec<?> buildSearchForDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), SEARCH_FOR_DOCUMENT_INTERACTION, task.getConversationId());
    }

    public WebClient.RequestHeadersSpec<?> buildRetrieveDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), RETRIEVE_DOCUMENT_INTERACTION, task.getConversationId());
    }

    private WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> buildRequest(String odsCode, String interaction,
        String conversationId) {
        var sslContext = requestBuilderService.buildSSLContext();
        var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return buildWebClient(httpClient)
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(ENDPOINT)
                .queryParam(ORG_CODE_PARAMETER, ORG_CODE_IDENTIFIER + PIPE_ENCODED + odsCode)
                .queryParam(INTERACTION_PARAMETER, INTERACTION_IDENTIFIER + PIPE_ENCODED + interaction)
                .build())
            .header(API_KEY_HEADER, sdsConfiguration.getApiKey())
            .header(X_CORRELATION_ID_HEADER, conversationId);
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filters(this::addWebClientFilters)
            .baseUrl(sdsConfiguration.getUrl())
            .build();
    }

    private void addWebClientFilters(List<ExchangeFilterFunction> filters) {
        filters.add(webClientFilterService.errorHandlingFilter(WebClientFilterService.RequestType.SDS, HttpStatus.OK));
        filters.add(webClientFilterService.logRequest());
    }
}
