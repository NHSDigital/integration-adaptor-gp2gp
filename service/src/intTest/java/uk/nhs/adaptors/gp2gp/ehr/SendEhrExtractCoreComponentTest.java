package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {
    private static final String PAYLOAD = "payload";

    private final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();

    @Mock
    private StorageDataWrapper storageDataWrapper;
    @MockBean
    private WebClient.RequestHeadersSpec<?> request;
    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;
    @MockBean
    private MhsClient mhsClient;
    @MockBean
    private SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition;
    @MockBean
    private StorageConnectorService storageConnectorService;
    @Autowired
    private SendEhrExtractCoreTaskExecutor sendEhrExtractCoreTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_NewExtractCoreTask_Expect_DatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(storageConnectorService.downloadFile(any())).thenReturn(storageDataWrapper);
        when(storageDataWrapper.getData()).thenReturn(PAYLOAD);
        when(sendEhrExtractCoreTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendEhrExtractCoreTaskDefinition.getTaskId()).thenReturn(randomIdGeneratorService.createNewId());
        when(mhsClient.sendMessageToMHS(request)).thenReturn("Successful Mhs Outbound Request");

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var ehrExtractCore = ehrExtractStatusUpdated.getEhrExtractCore();
        assertThat(ehrExtractCore.getSentAt()).isNotNull();
        assertThat(ehrExtractCore.getTaskId()).isNotNull();
    }

    @Test
    public void When_ExtractCoreThrowsException_Expect_EhrExtractStatusNotUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(sendEhrExtractCoreTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(storageConnectorService.downloadFile(any())).thenReturn(storageDataWrapper);
        when(storageDataWrapper.getData()).thenReturn(PAYLOAD);
        when(sendEhrExtractCoreTaskDefinition.getTaskId()).thenReturn(randomIdGeneratorService.createNewId());
        doThrow(InvalidOutboundMessageException.class)
            .when(mhsRequestBuilder).buildSendEhrExtractCoreRequest(any(), any(), any());

        assertThrows(InvalidOutboundMessageException.class, () -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtractUpdated.getEhrExtractCore()).isNull();
    }
}
