package uk.nhs.adaptors.gp2gp.gpc.builder;

import ca.uhn.fhir.parser.IParser;
import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.common.service.RequestBuilderService;
import uk.nhs.adaptors.gp2gp.common.service.WebClientFilterService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;

import java.util.Collections;
import java.util.List;

import static java.lang.String.valueOf;
import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcRequestBuilder {
    @Value("${gp2gp.gpc.overrideFromAsid}")
    private String overrideFromAsid;
    @Value("${gp2gp.gpc.overrideToAsid}")
    private String overrideToAsid;

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";
    private static final String SSP_FROM = "Ssp-From";
    private static final String SSP_TO = "Ssp-To";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String SSP_TRACE_ID = "Ssp-TraceID";
    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_BEARER = "Bearer ";
    private static final int NUMBER_OF_RECENT_CONSULTANTS = 3;
    private static final String GPC_STRUCTURED_INTERACTION_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc"
        + ".getstructuredrecord-1";
    private static final String GPC_DOCUMENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";
    private static final String GPC_PATIENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    private static final String GPC_DOCUMENT_SEARCH_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    private static final String GPC_DOCUMENT_REFERENCE_INCLUDES = "/DocumentReference?_include=DocumentReference%3Asubject%3APatient"
        + "&_include=DocumentReference%3Acustodian%3AOrganization&_include=DocumentReference%3Aauthor%3AOrganization"
        + "&_include=DocumentReference%3Aauthor%3APractitioner&_revinclude%3Arecurse=PractitionerRole%3Apractitioner";
    private static final String IDENTIFIER_PARAMETER = "identifier";
    private static final String GPC_FIND_PATIENT_IDENTIFIER = NHS_NUMBER_SYSTEM + "|";

    private final IParser fhirParser;
    private final GpcTokenBuilder gpcTokenBuilder;
    private final GpcConfiguration gpcConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    @Value("${gp2gp.gpc.overrideNhsNumber}")
    private String overrideNhsNumber;

    public Parameters buildGetStructuredRecordRequestBody(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return new Parameters()
            .addParameter(buildParameterComponent("patientNHSNumber")
                .setValue(new Identifier().setSystem(NHS_NUMBER_SYSTEM).setValue(
                    ((overrideNhsNumber.isBlank()) ? structuredTaskDefinition.getNhsNumber() : overrideNhsNumber))))
            .addParameter(buildParameterComponent("includeAllergies")
                .addPart(buildParameterComponent("includeResolvedAllergies")
                    .setValue(new BooleanType(true))))
            .addParameter(buildParameterComponent("includeMedication"))
            .addParameter(buildParameterComponent("includeConsultations")
                .addPart(buildParameterComponent("includeNumberOfMostRecent")
                    .setValue(new IntegerType(NUMBER_OF_RECENT_CONSULTANTS))))
            .addParameter(buildParameterComponent("includeProblems"))
            .addParameter(buildParameterComponent("includeImmunisations"))
            .addParameter(buildParameterComponent("includeUncategorisedData"))
            .addParameter(buildParameterComponent("includeInvestigations"))
            .addParameter(buildParameterComponent("includeReferrals"));
    }

    private ParametersParameterComponent buildParameterComponent(String parameterName) {
        return new ParametersParameterComponent()
            .setName(parameterName);
    }

    public RequestHeadersSpec<?> buildGetStructuredRecordRequest(Parameters requestBodyParameters,
        GetGpcStructuredTaskDefinition structuredTaskDefinition, String gpcBaseUrl) {
        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = buildHttpClient(sslContext);
        WebClient client = buildWebClient(httpClient, gpcBaseUrl);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(gpcConfiguration.getStructuredEndpoint());

        var requestBody = fhirParser.encodeResourceToString(requestBodyParameters);
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter
            = BodyInserters.fromValue(requestBody);

        return buildRequestWithHeadersAndBody(uri, requestBody, bodyInserter, structuredTaskDefinition, GPC_STRUCTURED_INTERACTION_ID);
    }

    public RequestHeadersSpec<?> buildGetDocumentRecordRequest(GetGpcDocumentTaskDefinition documentTaskDefinition, String gpcBaseUrl) {
        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = buildHttpClient(sslContext);
        WebClient client = buildWebClient(httpClient, gpcBaseUrl);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.GET)
            .uri(documentTaskDefinition.getAccessDocumentUrl());

        return buildRequestWithHeaders(uri, documentTaskDefinition, GPC_DOCUMENT_INTERACTION_ID);
    }

    public RequestHeadersSpec<?> buildGetPatientIdentifierRequest(GetGpcStructuredTaskDefinition patientIdentifierTaskDefinition,
        String gpcBaseUrl) {
        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = buildHttpClient(sslContext);
        WebClient client = buildWebClient(httpClient, gpcBaseUrl);

        WebClient.RequestBodySpec uri = preparePatientUri(client, patientIdentifierTaskDefinition.getNhsNumber());

        return buildRequestWithHeaders(uri, patientIdentifierTaskDefinition, GPC_PATIENT_INTERACTION_ID);
    }

    public RequestHeadersSpec<?> buildGetPatientDocumentReferences(
            GetGpcStructuredTaskDefinition documentReferencesTaskDefinition, String patientId, String gpcBaseUrl) {

        SslContext sslContext = requestBuilderService.buildSSLContext();
        HttpClient httpClient = buildHttpClient(sslContext);
        WebClient client = buildWebClient(httpClient, gpcBaseUrl);

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(gpcBaseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.GET)
            .uri(factory.expand(gpcConfiguration.getPatientEndpoint() + "/"
                + patientId + GPC_DOCUMENT_REFERENCE_INCLUDES));

        return buildRequestWithHeaders(uri, documentReferencesTaskDefinition, GPC_DOCUMENT_SEARCH_ID);
    }

    private HttpClient buildHttpClient(SslContext sslContext) {
        return HttpClient.create()
            .secure(t -> t.sslContext(sslContext));
    }

    private WebClient buildWebClient(HttpClient httpClient, String baseUrl) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filters(this::addWebClientFilters)
            .baseUrl(baseUrl)
            .defaultUriVariables(Collections.singletonMap("url", baseUrl))
            .build();
    }

    private RequestBodySpec buildRequestWithHeaders(RequestBodySpec uri, TaskDefinition taskDefinition, String interactionId) {
        return uri.accept(MediaType.valueOf(FHIR_CONTENT_TYPE))
            .header(SSP_FROM, getFromAsid(taskDefinition.getFromAsid()))
            .header(SSP_TO, getToAsid(taskDefinition.getToAsid()))
            .header(SSP_INTERACTION_ID, interactionId)
            .header(SSP_TRACE_ID, taskDefinition.getConversationId())
            .header(AUTHORIZATION, AUTHORIZATION_BEARER + gpcTokenBuilder.buildToken(taskDefinition.getFromOdsCode()))
            .header(CONTENT_TYPE, FHIR_CONTENT_TYPE);
    }

    private String getFromAsid(String fromAsid) {
        if (StringUtils.isNotBlank(overrideFromAsid)) {
            LOGGER.warn("GP2GP_GPC_OVERRIDE_FROM_ASID is being used, no longer using provided from asid");
            return overrideFromAsid;
        } else {
            return fromAsid;
        }
    }

    private String getToAsid(String toAsid) {
        if (StringUtils.isNotBlank(overrideToAsid)) {
            LOGGER.warn("GP2GP_GPC_OVERRIDE_TO_ASID is being used, no longer using provided to asid");
            return overrideToAsid;
        } else {
            return toAsid;
        }
    }

    private RequestHeadersSpec<?> buildRequestWithHeadersAndBody(RequestBodySpec uri, String requestBody,
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter, TaskDefinition taskDefinition, String interactionId) {
        return buildRequestWithHeaders(uri, taskDefinition, interactionId)
            .body(bodyInserter)
            .header(CONTENT_LEN, valueOf(requestBody.length()));
    }

    private void addWebClientFilters(List<ExchangeFilterFunction> filters) {
        filters.add(webClientFilterService.errorHandlingFilter(WebClientFilterService.RequestType.GPC, HttpStatus.OK));
        filters.add(webClientFilterService.logRequest());
    }

    private WebClient.RequestBodySpec preparePatientUri(WebClient client, String nhsNumber) {
        return client
            .method(HttpMethod.GET)
            .uri(uriBuilder -> uriBuilder
                .path(gpcConfiguration.getPatientEndpoint())
                .queryParam(IDENTIFIER_PARAMETER, GPC_FIND_PATIENT_IDENTIFIER
                    + ((overrideNhsNumber.isBlank()) ? nhsNumber : overrideNhsNumber))
                .build(false));
    }
}
