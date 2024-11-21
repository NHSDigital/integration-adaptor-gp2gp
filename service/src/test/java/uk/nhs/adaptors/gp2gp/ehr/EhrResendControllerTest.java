package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.nhs.adaptors.gp2gp.common.configuration.ObjectMapperBean;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EhrResendControllerTest {

    private static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    private static final String URI_TYPE = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";
    private static final String CONVERSATION_ID = "123-456";
    private static final String NHS_NUMBER = "12345";
    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";
    private static final String INCUMBENT_NACK_CODE = "99";
    private static final String INCUMBENT_NACK_DISPLAY = "Unexpected condition.";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String GPCONNECT_ERROR_OR_WARNING_CODE = "http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1";
    private static final String INVALID_IDENTIFIER_VALUE = "INVALID_IDENTIFIER_VALUE";

    private ObjectMapper objectMapper;

    @Mock
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Mock
    private TimestampService timestampService;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @Mock
    private TaskDispatcher taskDispatcher;

    private EhrResendController ehrResendController;


    @BeforeEach
    void setUp() {
        ObjectMapperBean objectMapperBean = new ObjectMapperBean();
        objectMapper = objectMapperBean.objectMapper(new Jackson2ObjectMapperBuilder());
        FhirParseService fhirParseService = new FhirParseService();
        ehrResendController = new EhrResendController(ehrExtractStatusRepository,
                                                      taskDispatcher,
                                                      randomIdGeneratorService,
                                                      timestampService,
                                                      fhirParseService);
    }

    @Test
    void When_AnEhrExtractHasFailed_Expect_GetGpcStructuredTaskScheduled() {

        String ehrMessageRef = generateRandomUppercaseUUID();
        var ehrExtractStatus = new EhrExtractStatus();

        ehrExtractStatus.setConversationId(CONVERSATION_ID);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                                                           .conversationClosed(FIVE_DAYS_AGO)
                                                           .errors(List.of(
                                                               EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                                                                   .code(INCUMBENT_NACK_CODE)
                                                                   .display(INCUMBENT_NACK_DISPLAY)
                                                                   .build()))
                                                           .messageRef(ehrMessageRef)
                                                           .received(FIVE_DAYS_AGO)
                                                           .rootId(generateRandomUppercaseUUID())
                                                           .build());
        ehrExtractStatus.setEhrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).build());
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatus.setEhrContinue(EhrExtractStatus.EhrContinue.builder().build());
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder().build());

        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(CONVERSATION_ID);

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        doReturn(now).when(timestampService).now();

        ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());
        var taskDefinition = GetGpcStructuredTaskDefinition.getGetGpcStructuredTaskDefinition(randomIdGeneratorService, ehrExtractStatus);

        verify(taskDispatcher, times(1)).createTask(taskDefinition);
        assertEquals(now, updatedEhrExtractStatus.get().getMessageTimestamp());
        assertEquals(now, updatedEhrExtractStatus.get().getCreated());
        assertEquals(now, updatedEhrExtractStatus.get().getUpdatedAt());
        assertNull(updatedEhrExtractStatus.get().getEhrExtractCorePending());
        assertNull(updatedEhrExtractStatus.get().getEhrContinue());
        assertNull(updatedEhrExtractStatus.get().getAckPending());
        assertNull(updatedEhrExtractStatus.get().getEhrReceivedAcknowledgement());
        assertNull(updatedEhrExtractStatus.get().getGpcAccessDocument());
    }

    @Test
    void When_AnEhrExtractHasNotFailedAndAnotherResendRequestArrives_Expect_FailedOperationOutcome() throws JsonProcessingException {

        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem(GPCONNECT_ERROR_OR_WARNING_CODE);
        codeableConceptCoding.setCode(INTERNAL_SERVER_ERROR);
        details.setCoding(List.of(codeableConceptCoding));

        final EhrExtractStatus IN_PROGRESS_EXTRACT_STATUS = EhrExtractStatus.builder()
            .conversationId(CONVERSATION_ID)
            .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
            .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
            .ehrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
            .build();

        doReturn(Optional.of(IN_PROGRESS_EXTRACT_STATUS)).when(ehrExtractStatusRepository).findByConversationId(CONVERSATION_ID);


        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode jsonCodingSection = rootNode.path("issue").get(0).path("details").path("coding").get(0);
        var code = jsonCodingSection.path("code").asText();
        var system = jsonCodingSection.path("system").asText();
        var operationOutcomeUrl = rootNode.path("meta").path("profile").get(0).asText();

        assertEquals(INTERNAL_SERVER_ERROR, code);
        assertEquals(GPCONNECT_ERROR_OR_WARNING_CODE, system);
        assertEquals(URI_TYPE, operationOutcomeUrl);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void When_AnEhrExtractHasFailed_Expect_RespondsWith202() {

        String ehrMessageRef = generateRandomUppercaseUUID();
        var ehrExtractStatus = new EhrExtractStatus();

        ehrExtractStatus.setConversationId(CONVERSATION_ID);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                                                           .conversationClosed(FIVE_DAYS_AGO)
                                                           .errors(List.of(
                                                               EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
                                                                   .code(INCUMBENT_NACK_CODE)
                                                                   .display(INCUMBENT_NACK_DISPLAY)
                                                                   .build()))
                                                           .messageRef(ehrMessageRef)
                                                           .received(FIVE_DAYS_AGO)
                                                           .rootId(generateRandomUppercaseUUID())
                                                           .build());
        ehrExtractStatus.setEhrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).build());

        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(CONVERSATION_ID);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertNull(response.getBody());
    }

    @Test
    void When_AnEhrExtractHasNotFailed_Expect_RespondsWith403() throws JsonProcessingException {

        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem(GPCONNECT_ERROR_OR_WARNING_CODE);
        codeableConceptCoding.setCode(INTERNAL_SERVER_ERROR);
        details.setCoding(List.of(codeableConceptCoding));

        String ehrMessageRef = generateRandomUppercaseUUID();
        var ehrExtractStatus = new EhrExtractStatus();

        ehrExtractStatus.setConversationId(CONVERSATION_ID);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                                                           .conversationClosed(FIVE_DAYS_AGO)
                                                           .errors(List.of())
                                                           .messageRef(ehrMessageRef)
                                                           .received(FIVE_DAYS_AGO)
                                                           .rootId(generateRandomUppercaseUUID())
                                                           .build());
        ehrExtractStatus.setEhrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).build());

        doReturn(Optional.of(ehrExtractStatus)).when(ehrExtractStatusRepository).findByConversationId(CONVERSATION_ID);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode jsonCodingSection = rootNode.path("issue").get(0).path("details").path("coding").get(0);
        var code = jsonCodingSection.path("code").asText();
        var system = jsonCodingSection.path("system").asText();
        var operationOutcomeUrl = rootNode.path("meta").path("profile").get(0).asText();

        assertEquals(INTERNAL_SERVER_ERROR, code);
        assertEquals(GPCONNECT_ERROR_OR_WARNING_CODE, system);
        assertEquals(URI_TYPE, operationOutcomeUrl);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void When_AnEhrExtractDoesNotExist_Expect_RespondsWith404() throws JsonProcessingException {

        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem(GPCONNECT_ERROR_OR_WARNING_CODE);
        codeableConceptCoding.setCode("INVALID_IDENTIFIER_VALUE");
        details.setCoding(List.of(codeableConceptCoding));

        doReturn(Optional.empty()).when(ehrExtractStatusRepository).findByConversationId(CONVERSATION_ID);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode jsonCodingSection = rootNode.path("issue").get(0).path("details").path("coding").get(0);
        var code = jsonCodingSection.path("code").asText();
        var system = jsonCodingSection.path("system").asText();
        var operationOutcomeUrl = rootNode.path("meta").path("profile").get(0).asText();

        assertEquals(INVALID_IDENTIFIER_VALUE, code);
        assertEquals(GPCONNECT_ERROR_OR_WARNING_CODE, system);
        assertEquals(URI_TYPE, operationOutcomeUrl);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    static OperationOutcome createOperationOutcome(
        OperationOutcome.IssueType type, OperationOutcome.IssueSeverity severity, CodeableConcept details, String diagnostics) {
        var operationOutcome = new OperationOutcome();
        Meta meta = new Meta();
        meta.setProfile(Collections.singletonList(new UriType(URI_TYPE)));
        operationOutcome.setMeta(meta);
        operationOutcome.addIssue()
            .setCode(type)
            .setSeverity(severity)
            .setDetails(details)
            .setDiagnostics(diagnostics);
        return operationOutcome;
    }

}
