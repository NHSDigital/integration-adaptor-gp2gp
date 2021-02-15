package uk.nhs.adaptors.gp2gp.sds.builder;

import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
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

    private static final String GPC_REQUEST_TYPE_FILTER = "Gpc";

    private final SdsConfiguration sdsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    public WebClient.RequestHeadersSpec<?> buildGetStructuredRecordRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), GET_STRUCTURED_INTERACTION);
    }

    public WebClient.RequestHeadersSpec<?> buildPatientSearchAccessDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION);
    }

    public WebClient.RequestHeadersSpec<?> buildSearchForDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), SEARCH_FOR_DOCUMENT_INTERACTION);
    }

    public WebClient.RequestHeadersSpec<?> buildRetrieveDocumentRequest(TaskDefinition task) {
        return buildRequest(task.getFromOdsCode(), RETRIEVE_DOCUMENT_INTERACTION);
    }

    private WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> buildRequest(String odsCode, String interaction) {
        var sslContext = requestBuilderService.buildSSLContext();
        var httpClient = buildHttpClient(sslContext);
        return buildWebClient(httpClient)
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(ENDPOINT)
                .queryParam(ORG_CODE_PARAMETER, ORG_CODE_IDENTIFIER + PIPE_ENCODED + odsCode)
                .queryParam(INTERACTION_PARAMETER, INTERACTION_IDENTIFIER + PIPE_ENCODED + interaction)
                .build())
            .header(API_KEY_HEADER, sdsConfiguration.getApiKey());
    }

    private HttpClient buildHttpClient(SslContext sslContext) {
        var httpClient =  HttpClient.create()
            .secure(t -> t.sslContext(sslContext));

        if (sdsConfiguration.isEnableProxy()) {
            LOGGER.info("Using HTTP Proxy {}:{} for SDS API", sdsConfiguration.getProxy(), sdsConfiguration.getProxyPort());
            return httpClient
                .proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(sdsConfiguration.getProxy())
                    .port(Integer.parseInt(sdsConfiguration.getProxyPort())));
        } else {
            return httpClient;
        }
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
        filters.add(webClientFilterService.errorHandlingFilter(GPC_REQUEST_TYPE_FILTER, HttpStatus.OK));
        filters.add(webClientFilterService.logRequest());
    }
}
