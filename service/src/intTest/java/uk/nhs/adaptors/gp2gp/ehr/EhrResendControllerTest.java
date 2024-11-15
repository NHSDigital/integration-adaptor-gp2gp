package uk.nhs.adaptors.gp2gp.ehr;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_DISPLAY;

@SpringBootTest
@DirtiesContext
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
public class EhrResendControllerTest {

    public static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));
    private static final String URI_TYPE = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";
    private static final String CONVERSATION_ID = "123-456";
    public static final String NHS_NUMBER = "12345";
    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private EhrResendController ehrResendController;

    @MockBean
    private RandomIdGeneratorService randomIdGeneratorService;

    @MockBean
    private TaskDispatcher taskDispatcher;

    @MockBean
    private TimestampService timestampService;

    @Test
    public void When_AnEhrExtractHasNotFailedAndAnotherResendRequestArrives_Expect_FailedOperationOutcome() {

        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem("http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1");
        codeableConceptCoding.setCode("INTERNAL_SERVER_ERROR");
        details.setCoding(List.of(codeableConceptCoding));
        var diagnostics = "The current resend operation is still in progress. Please wait for it to complete before retrying";

        final EhrExtractStatus IN_PROGRESS_EXTRACT_STATUS = EhrExtractStatus.builder()
            .conversationId(CONVERSATION_ID)
            .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
            .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
            .ehrRequest(EhrExtractStatus.EhrRequest.builder().nhsNumber(NHS_NUMBER).toAsid(TO_ASID_CODE).fromAsid(FROM_ASID_CODE).build())
            .build();

        ehrExtractStatusRepository.save(IN_PROGRESS_EXTRACT_STATUS);

        var operationOutcome = createOperationOutcome(IssueType.BUSINESSRULE, OperationOutcome.IssueSeverity.ERROR, details, diagnostics);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(operationOutcome);
    }

    @Test
    public void When_AnEhrExtractHasFailed_Expect_RespondsWith202() {

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

        ehrExtractStatusRepository.save(ehrExtractStatus);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void When_AnEhrExtractHasFailed_Expect_GetGpcStructuredTaskScheduled() {

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

        ehrExtractStatusRepository.save(ehrExtractStatus);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        doReturn(now).when(timestampService).now();

        ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());
        var taskDefinition = GetGpcStructuredTaskDefinition.getGetGpcStructuredTaskDefinition(randomIdGeneratorService, ehrExtractStatus);

        verify(taskDispatcher, times(1)).createTask(taskDefinition);
        assertEquals(now, updatedEhrExtractStatus.get().getMessageTimestamp());
        assertNull(updatedEhrExtractStatus.get().getEhrExtractCorePending());
        assertNull(updatedEhrExtractStatus.get().getEhrContinue());
        assertNull(updatedEhrExtractStatus.get().getAckPending());
        assertNull(updatedEhrExtractStatus.get().getEhrReceivedAcknowledgement());
    }

    @Test
    public void When_AnEhrExtractDoesNotExist_Expect_RespondsWith404() {

        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem("http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1");
        codeableConceptCoding.setCode("INVALID_IDENTIFIER_VALUE");
        details.setCoding(List.of(codeableConceptCoding));
        var diagnostics = "Provide a conversationId that exists and retry the operation";

        var operationOutcome = createOperationOutcome(IssueType.VALUE, OperationOutcome.IssueSeverity.ERROR, details, diagnostics);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(operationOutcome);
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    public static OperationOutcome createOperationOutcome(
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
