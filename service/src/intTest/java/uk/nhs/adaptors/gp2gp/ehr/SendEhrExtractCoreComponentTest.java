package uk.nhs.adaptors.gp2gp.ehr;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {
    @Mock
    private MhsClient mhsClient;
    @Mock
    private WebClient.RequestHeadersSpec<?> request;
    @Mock
    private SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition;

    @Autowired
    @InjectMocks
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

    @Test
    public void When_ExtractCoreThrowsException_Expect_EhrExtractStatusNotUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        assertThrows(EhrExtractException.class, () -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtractUpdated.getEhrExtractCore()).isNull();
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
