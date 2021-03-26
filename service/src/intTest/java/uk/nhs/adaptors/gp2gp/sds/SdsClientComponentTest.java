package uk.nhs.adaptors.gp2gp.sds;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.ResourceReader;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.sds.configuration.SdsConfiguration;
import uk.nhs.adaptors.gp2gp.sds.exception.SdsException;
import uk.nhs.adaptors.gp2gp.testcontainers.WiremockExtension;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith({SpringExtension.class, WiremockExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SdsClientComponentTest {

    private static final String FROM_ODS_CODE = "ABC123";
    private static final String ADDRESS = "http://test/";

    private static final String GET_STRUCTURED_INTERACTION =
        "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    private static final String SEARCH_FOR_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    private static final String RETRIEVE_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";

    private static final TaskDefinition TASK_DEFINITION = GetGpcStructuredTaskDefinition.builder()
        .fromOdsCode(FROM_ODS_CODE)
        .conversationId(String.valueOf(UUID.randomUUID()))
        .build();

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SdsConfiguration sdsConfiguration;

    @Autowired
    private SdsClient sdsClient;

    @Value("classpath:sds/sds_response.json")
    private Resource sdsResponse;

    @Value("classpath:sds/sds_no_result_response.json")
    private Resource sdsNoResultResponse;

    @Value("classpath:sds/sds_no_address_response.json")
    private Resource sdsNoAddressResponse;

    @Value("classpath:sds/sds_error_response.json")
    private Resource sdsErrorResponse;

    private List<Pair<String, Function<TaskDefinition, Optional<SdsClient.SdsResponseData>>>> allInteractions;

    @PostConstruct
    public void postConstruct() {
        allInteractions = List.of(
            Pair.of(GET_STRUCTURED_INTERACTION, sdsClient::callForGetStructuredRecord),
            Pair.of(PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION, sdsClient::callForPatientSearchAccessDocument),
            Pair.of(SEARCH_FOR_DOCUMENT_INTERACTION, sdsClient::callForSearchForDocumentRecord),
            Pair.of(RETRIEVE_DOCUMENT_INTERACTION, sdsClient::callForRetrieveDocumentRecord));
    }

    @BeforeAll
    public static void beforeAll() {
        WireMock.configureFor(Integer.parseInt(System.getProperty("GP2GP_WIREMOCK_PORT")));
    }

    private void stubEndpoint(String interaction, String response) {
        stubFor(get(urlPathEqualTo("/Endpoint"))
                .withQueryParam("organization", equalTo("https://fhir.nhs.uk/Id/ods-organization-code%7C" + FROM_ODS_CODE))
                .withQueryParam("identifier", equalTo("https://fhir.nhs.uk/Id/nhsServiceInteractionId%7C" + interaction))
                .withHeader("apikey", matching(".*"))
                .withHeader("X-Correlation-Id", matching("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(response)));
    }

    private void stubEndpointError() {
        stubFor(get(urlPathEqualTo("/Endpoint"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.UNAUTHORIZED.value())
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(ResourceReader.asString(sdsErrorResponse))));
    }

    @Test
    public void When_SdsReturnsResult_Expect_AddressIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsResponse));
            var retrievedSdsData = pair.getValue().apply(TASK_DEFINITION);
            assertThat(retrievedSdsData)
                .isNotEmpty()
                .hasValue(SdsClient.SdsResponseData.builder().address(ADDRESS).build());
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsNoResult_Expect_EmptyResultIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsNoResultResponse));
            var retrievedSdsData = pair.getValue().apply(TASK_DEFINITION);
            assertThat(retrievedSdsData)
                .isEmpty();
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsEmptyAddress_Expect_EmptyResultIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsNoAddressResponse));
            var retrievedSdsData = pair.getValue().apply(TASK_DEFINITION);
            assertThat(retrievedSdsData)
                .isEmpty();
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsError_Expect_Exception() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(TASK_DEFINITION))
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsRequestWithInvalidXCorrelationId_Expect_Exception() {
        TaskDefinition taskInvalidUUID = GetGpcStructuredTaskDefinition.builder()
            .fromOdsCode(FROM_ODS_CODE)
            .conversationId("test")
            .build();

        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(taskInvalidUUID))
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsRequestWithNoXCorrelationId_Expect_Exception() {
        TaskDefinition taskNoConversationId= GetGpcStructuredTaskDefinition.builder()
            .fromOdsCode(FROM_ODS_CODE)
            .build();

        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(taskNoConversationId))
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }
}
