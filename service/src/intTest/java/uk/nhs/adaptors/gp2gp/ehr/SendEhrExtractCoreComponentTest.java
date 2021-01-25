package uk.nhs.adaptors.gp2gp.ehr;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {

    @Autowired
    private SendEhrExtractCoreTaskExecutor sendEhrExtractCoreTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_NewExtractCoreTask_Expect_DatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition = buildValidExtractCoreTask(ehrExtractStatus);
        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);
    }

    private SendEhrExtractCoreTaskDefinition buildValidExtractCoreTask(EhrExtractStatus ehrExtractStatus) {
        return SendEhrExtractCoreTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .build();
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var gpcAccessStructured = ehrExtractStatusUpdated.getEhrExtractCore();
        assertThat(gpcAccessStructured.getSentAt()).isNotNull();
        assertThat(gpcAccessStructured.getTaskId()).isNotNull();
    }
}
