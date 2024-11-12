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
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
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

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private EhrResendController ehrResendController;

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

        ehrExtractStatusRepository.save(ehrExtractStatus);

        var response = ehrResendController.scheduleEhrExtractResend(CONVERSATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
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
