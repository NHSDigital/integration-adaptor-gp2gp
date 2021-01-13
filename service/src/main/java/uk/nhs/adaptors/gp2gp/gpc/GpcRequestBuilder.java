package uk.nhs.adaptors.gp2gp.gpc;

import static java.lang.String.valueOf;

import static org.apache.http.protocol.HTTP.CONTENT_LEN;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static org.apache.http.protocol.HTTP.TARGET_HOST;

import java.util.Collections;

import javax.net.ssl.SSLException;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import ca.uhn.fhir.parser.IParser;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcRequestBuilder {

    private static final String NHS_NUMBER_SYSTEM = "https://fhir.nhs.uk/Id/nhs-number";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";
    private static final String SSP_FROM = "Ssp-From";
    private static final String SSP_TO = "Ssp-To";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String SSP_TRACE_ID = "Ssp-TraceID";
    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_BEARER = "Bearer ";
    private static final int BYTE_COUNT = 16 * 1024 * 1024;
    private static final int NUMBER_OF_RECENT_CONSULTANTS = 3;

    private final IParser fhirParser;
    private final GpcTokenBuilder gpcTokenBuilder;
    private final GpcConfiguration gpcConfiguration;
    private final GpcWebClientFilter gpcWebClientFilter;

    public Parameters buildGetStructuredRecordRequestBody(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        return new Parameters()
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("patientNHSNumber")
                .setValue(new Identifier().setSystem(NHS_NUMBER_SYSTEM).setValue(structuredTaskDefinition.getNhsNumber())))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeAllergies")
                .addPart(new Parameters.ParametersParameterComponent()
                    .setName("includeResolvedAllergies")
                    .setValue(new BooleanType(true))))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeMedication"))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeConsultations")
                .addPart(new Parameters.ParametersParameterComponent()
                    .setName("includeNumberOfMostRecent")
                    .setValue(new IntegerType(NUMBER_OF_RECENT_CONSULTANTS))))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeProblems"))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeImmunisations"))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeUncategorisedData"))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeInvestigations"))
            .addParameter(new Parameters.ParametersParameterComponent()
                .setName("includeReferrals"));
    }

    public WebClient.RequestHeadersSpec<?> buildGetStructuredRecordRequest(Parameters requestBodyParameters,
        EhrExtractStatus ehrExtractStatus) throws SSLException {
        SslContext sslContext = buildSSLContext();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient client = buildWebClient(httpClient);

        WebClient.RequestBodySpec uri = client
            .method(HttpMethod.POST)
            .uri(gpcConfiguration.getEndpoint());

        var requestBody = fhirParser.encodeResourceToString(requestBodyParameters);
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter
            = BodyInserters.fromValue(requestBody);

        var request = ehrExtractStatus.getEhrRequest();

        return uri
            .body(bodyInserter)
            .accept(MediaType.valueOf(FHIR_CONTENT_TYPE))
            .header(SSP_FROM, request.getFromAsid())
            .header(SSP_TO, request.getToAsid())
            .header(SSP_INTERACTION_ID, "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1")
            .header(SSP_TRACE_ID, ehrExtractStatus.getConversationId())
            .header(AUTHORIZATION, AUTHORIZATION_BEARER + gpcTokenBuilder.buildToken(ehrExtractStatus.getEhrRequest().getFromOdsCode()))
            .header(TARGET_HOST, gpcConfiguration.getHost())
            .header(CONTENT_LEN, valueOf(requestBody.length()))
            .header(CONTENT_TYPE, FHIR_CONTENT_TYPE);
    }

    private SslContext buildSSLContext() throws SSLException {
        return SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
    }

    private org.springframework.web.reactive.function.client.WebClient buildWebClient(HttpClient httpClient) {
        return org.springframework.web.reactive.function.client.WebClient
            .builder()
            .exchangeStrategies(
                ExchangeStrategies
                    .builder()
                    .codecs(
                        configurer -> configurer.defaultCodecs()
                            .maxInMemorySize(BYTE_COUNT)).build())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(gpcWebClientFilter.errorHandlingFilter())
            .baseUrl(gpcConfiguration.getUrl())
            .defaultUriVariables(Collections.singletonMap("url", gpcConfiguration.getUrl()))
            .build();
    }
}

