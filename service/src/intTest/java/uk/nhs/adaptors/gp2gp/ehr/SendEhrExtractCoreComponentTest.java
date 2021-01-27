package uk.nhs.adaptors.gp2gp.ehr;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {
    @MockBean
    private WebClient.RequestHeadersSpec<?> request;
    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;
    @MockBean
    private MhsClient mhsClient;
    @MockBean
    private SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition;

    @Autowired
    private SendEhrExtractCoreTaskExecutor sendEhrExtractCoreTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_NewExtractCoreTask_Expect_DatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(sendEhrExtractCoreTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendEhrExtractCoreTaskDefinition.getTaskId()).thenReturn(UUID.randomUUID().toString());
        when(mhsClient.sendEhrExtractCore(request, sendEhrExtractCoreTaskDefinition)).thenReturn("Successful Mhs Outbound Request");

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);
    }

    @Test
    public void When_ExtractCoreThrowsException_Expect_EhrExtractStatusNotUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(sendEhrExtractCoreTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendEhrExtractCoreTaskDefinition.getTaskId()).thenReturn(UUID.randomUUID().toString());
        when(mhsClient.sendEhrExtractCore(request, sendEhrExtractCoreTaskDefinition)).thenThrow(InvalidOutboundMessageException.class);

        assertThrows(InvalidOutboundMessageException.class, () -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

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
