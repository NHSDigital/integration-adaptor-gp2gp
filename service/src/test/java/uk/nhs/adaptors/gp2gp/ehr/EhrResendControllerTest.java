package uk.nhs.adaptors.gp2gp.ehr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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
import uk.nhs.adaptors.gp2gp.ehr.status.controller.EhrStatusController;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

@ExtendWith(MockitoExtension.class)
class EhrResendControllerTest {

    public static final String DIAGNOSTICS_MSG
         = "The current patient transfer operation is still in progress. "
            + "The conversation can only be resent after a negative acknowledgement is sent/received, "
            + "or no acknowledgement is received from the requesting side 8 days after sending the EHR.";
    private static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    private static final String URI_TYPE = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";
    private static final String CONVERSATION_ID = "123-456";
    private static final String NHS_NUMBER = "12345";
    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";
    private static final String INCUMBENT_NACK_CODE = "99";
    private static final String INCUMBENT_NACK_DISPLAY = "Unexpected condition.";
    public static final List<EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails> EHR_RECEIVED_ACK_ERROR_DETAILS = List.of(
        EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails.builder()
            .code(INCUMBENT_NACK_CODE)
            .display(INCUMBENT_NACK_DISPLAY)
            .build());
    private static final String PRECONDITION_FAILED = "PRECONDITION_FAILED";
    private static final String GPCONNECT_ERROR_OR_WARNING_CODE = "https://fhir.nhs.uk/STU3/ValueSet/Spine-ErrorOrWarningCode-1";
    private static final String INVALID_IDENTIFIER_VALUE = "INVALID_IDENTIFIER_VALUE";
    public static final String ISSUE_CODE_VALUE = "value";
    public static final String ISSUE_CODE_BUSINESS_RULE = "business-rule";

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
    private EhrStatusController ehrStatusController;
    private EhrStatusService ehrStatusService;


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
        ehrStatusService = new EhrStatusService(ehrExtractStatusRepository);
        ehrStatusController = new EhrStatusController(ehrStatusService);
    }

    @Test
    void When_AnEhrExtractHasFailed_Expect_GetGpcStructuredTaskScheduledAndEhrExtractStatusIsReset() {

        var ehrExtractStatus = getEhrExtractStatus(EHR_RECEIVED_ACK_ERROR_DETAILS);
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatus.setEhrContinue(EhrExtractStatus.EhrContinue.builder().build());
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder().build());
        ehrExtractStatus.setCreated(FIVE_DAYS_AGO);
        ehrExtractStatus.setError(EhrExtractStatus.Error.builder().message("Failed to download EHR").build());

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.of(ehrExtractStatus));

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(timestampService.now()).thenReturn(now);

        ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        var taskDefinition = GetGpcStructuredTaskDefinition.getGetGpcStructuredTaskDefinition(randomIdGeneratorService, ehrExtractStatus);

        assertAll(
            () -> verify(taskDispatcher, times(1)).createTask(taskDefinition),
            () -> verify(ehrExtractStatusRepository, times(1)).save(ehrExtractStatus),
            () -> assertEquals(now, ehrExtractStatus.getMessageTimestamp()),
            () -> assertEquals(FIVE_DAYS_AGO, ehrExtractStatus.getCreated()),
            () -> assertEquals(now, ehrExtractStatus.getUpdatedAt()),
            () -> assertNull(ehrExtractStatus.getEhrExtractCorePending()),
            () -> assertNull(ehrExtractStatus.getEhrContinue()),
            () -> assertNull(ehrExtractStatus.getAckPending()),
            () -> assertNull(ehrExtractStatus.getEhrReceivedAcknowledgement()),
            () -> assertNull(ehrExtractStatus.getGpcAccessDocument()),
            () -> assertNull(ehrExtractStatus.getError())
        );
    }

    @Test
    void When_AnEhrExtractIsStillInProgress_Expect_FailedOperationOutcome() throws JsonProcessingException {

        final EhrExtractStatus IN_PROGRESS_EXTRACT_STATUS = EhrExtractStatus.builder()
            .conversationId(CONVERSATION_ID)
            .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
            .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
            .ehrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
            .build();

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.of(IN_PROGRESS_EXTRACT_STATUS));

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());

        assertAll(
            () -> assertResponseHasExpectedOperationOutcome(rootNode, PRECONDITION_FAILED, DIAGNOSTICS_MSG, ISSUE_CODE_BUSINESS_RULE),
            () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode())
        );
    }

    @Test
    void When_AnEhrExtractHasFailed_Expect_RespondsWith202() {

        var ehrExtractStatus = getEhrExtractStatus(EHR_RECEIVED_ACK_ERROR_DETAILS);

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.of(ehrExtractStatus));

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertAll(
            () -> assertEquals(HttpStatus.ACCEPTED, response.getStatusCode()),
            () -> assertNull(response.getBody())
        );
    }

    @Test
    void When_AnEhrExtractHasAPositiveAcknowledgement_Expect_FailedOperationOutcome() throws JsonProcessingException {

        var ehrExtractStatus = getEhrExtractStatus(List.of());

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.of(ehrExtractStatus));

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());

        assertAll(
            () -> assertResponseHasExpectedOperationOutcome(rootNode, PRECONDITION_FAILED, DIAGNOSTICS_MSG, ISSUE_CODE_BUSINESS_RULE),
            () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode())
        );
    }

    @Test
    void When_AnEhrExtractDoesNotExist_Expect_RespondsWith404() throws JsonProcessingException {

        var diagnosticsMsg = "Provide a conversationId that exists and retry the operation";

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.empty());

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        JsonNode rootNode = objectMapper.readTree(response.getBody());

        assertAll(
            () -> assertResponseHasExpectedOperationOutcome(rootNode, INVALID_IDENTIFIER_VALUE, diagnosticsMsg, ISSUE_CODE_VALUE),
            () -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode())
        );
    }

    @Test
    void When_ResendingTransfer_Expect_EhrStatusInProgressStatus() {

        var ehrExtractStatus = getEhrExtractStatus(EHR_RECEIVED_ACK_ERROR_DETAILS);
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatus.setEhrContinue(EhrExtractStatus.EhrContinue.builder().build());
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder().build());

        when(ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID)).thenReturn(Optional.of(ehrExtractStatus));

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(timestampService.now()).thenReturn(now);

        ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);
        var ehrStatus = ehrStatusController.getEhrStatus(CONVERSATION_ID);

        assertEquals(IN_PROGRESS, ehrStatus.getBody().getMigrationStatus());
    }

    private @NotNull EhrExtractStatus getEhrExtractStatus(List<EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails> errors) {

        String ehrMessageRef = generateRandomUppercaseUUID();
        var ehrExtractStatus = new EhrExtractStatus();

        ehrExtractStatus.setConversationId(CONVERSATION_ID);
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder()
                                                           .conversationClosed(FIVE_DAYS_AGO)
                                                           .errors(errors)
                                                           .messageRef(ehrMessageRef)
                                                           .received(FIVE_DAYS_AGO)
                                                           .rootId(generateRandomUppercaseUUID())
                                                           .build());
        ehrExtractStatus.setEhrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).build());
        return ehrExtractStatus;
    }

    private void assertResponseHasExpectedOperationOutcome(JsonNode rootNode, String serverErrMsg,
                                                           String diagnosticsMsg, String issueCode) {
        var coding = rootNode.path("issue").get(0).path("details").path("coding").get(0);
        assertAll(
            () -> assertEquals(serverErrMsg, coding.path("code").asText()),
            () -> assertEquals("error", rootNode.path("issue").get(0).path("severity").asText()),
            () -> assertEquals(issueCode, rootNode.path("issue").get(0).path("code").asText()),
            () -> assertEquals(GPCONNECT_ERROR_OR_WARNING_CODE, coding.path("system").asText()),
            () -> assertEquals(URI_TYPE, rootNode.path("meta").path("profile").get(0).asText()),
            () -> assertEquals(diagnosticsMsg, rootNode.path("issue").get(0).path("diagnostics").asText())
        );
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

}
