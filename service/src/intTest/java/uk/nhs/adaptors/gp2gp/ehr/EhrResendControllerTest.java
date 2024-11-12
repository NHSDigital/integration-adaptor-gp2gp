package uk.nhs.adaptors.gp2gp.ehr;

import org.hl7.fhir.dstu3.model.OperationOutcome;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_CODE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.INCUMBENT_NACK_DISPLAY;


@SpringBootTest
@DirtiesContext
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
public class EhrResendControllerTest {

    public static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIVE_DAYS_AGO = NOW.minus(Duration.ofDays(5));

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private EhrResendController ehrResendController;

    @Test
    public void When_AnEhrExtractHasFailed_Expect_RespondsWith202() {

        String ehrMessageRef = generateRandomUppercaseUUID();
        var ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setConversationId("123-456");
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

        var response = ehrResendController.scheduleEhrExtractResend("123-456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void When_AnEhrExtractDoesNotExist_Expect_RespondsWith404() {
        var response = ehrResendController.scheduleEhrExtractResend("123-456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // TODO: Add more detail to OperationOutcome
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(new OperationOutcome());
    }

    private String generateRandomUppercaseUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }


}
