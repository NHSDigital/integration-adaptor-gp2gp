package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendDocumentComponentTest {
    @Mock
    private StorageDataWrapper storageDataWrapper;
    @MockBean
    private StorageConnectorService storageConnectorService;
    @MockBean
    private WebClient.RequestHeadersSpec<?> request;
    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;
    @MockBean
    private MhsClient mhsClient;
    @MockBean
    private SendDocumentTaskDefinition sendDocumentTaskDefinition;
    @Autowired
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_SendDocumentTaskRunsTwice_Expect_DatabaseOverwritesEhrExtractStatus() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var ehrRequest = ehrExtractStatus.getEhrRequest();

        when(storageConnectorService.downloadFile(anyString())).thenReturn(storageDataWrapper);
        when(storageDataWrapper.getData()).thenReturn("payload");

        when(sendDocumentTaskDefinition.getDocumentName()).thenReturn("documentName.json");
        when(sendDocumentTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendDocumentTaskDefinition.getFromOdsCode()).thenReturn(ehrRequest.getFromOdsCode());
        when(sendDocumentTaskDefinition.getTaskId()).thenReturn(ehrRequest.getFromOdsCode());
        when(mhsClient.sendMessageToMHS(request)).thenReturn("Successful Mhs Outbound Request");

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractFirst = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractSecond = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(ehrExtractFirst.getUpdatedAt()).isNotEqualTo(ehrExtractSecond.getUpdatedAt());
    }
}
