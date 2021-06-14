package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class FailingProcessTest {

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private ProcessFailureHandlingService processFailureHandlingService;

    @Test
    public void When_CalledToFailProcess_Expect_DbToBeUpdatedWithError() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var errorCode = "errorCode1";
        var errorMessage = "errorMessage1";
        var taskType = "taskType1";

        processFailureHandlingService.failProcess(
            ehrExtractStatus.getConversationId(),
            errorCode,
            errorMessage,
            taskType
        );

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId());

        assertThat(updatedEhrExtractStatus.isPresent());

        var error = updatedEhrExtractStatus.get().getError();
        assertThat(error).isNotNull();
        assertThat(error.getOccurredAt()).isNotNull();
        assertThat(error.getCode()).isEqualTo(errorCode);
        assertThat(error.getMessage()).isEqualTo(errorMessage);
        assertThat(error.getTaskType()).isEqualTo(taskType);
    }
}
