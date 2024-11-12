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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
public class EhrResendControllerTest {
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private EhrResendController ehrResendController;

    @Test
    public void When_AnEhrExtractHasFailed_Expect_RespondsWith202() {
        var ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setConversationId("123-456");
        // TODO: Mark ehrExtractStatus as failed.
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
}
