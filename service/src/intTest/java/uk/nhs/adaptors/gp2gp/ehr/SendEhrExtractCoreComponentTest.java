package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.FROM_ODS_CODE;


import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;


import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.util.List;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {
    private static final String PAYLOAD = "payload";
    private static final String EXPECTED_STRUCTURED_RECORD_JSON_FILENAME =
        CONVERSATION_ID.concat("/").concat(CONVERSATION_ID).concat("_gpc_structured.json");
    public static final String PAYLOAD_WITH_LENGTH_PLACEHOLDER =
            "This is a payload. ${LENGTH_PLACEHOLDER_ID=" + DOCUMENT_ID + "}";

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
    private EhrExtractStatus ehrExtractStatus;

    @MockBean
    private SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;

    @Test
    public void When_NewExtractCoreTask_Expect_DatabaseUpdated() {
        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId())
                .orElseThrow();

        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);
    }

    @Test
    public void When_NewExtractCoreTask_Expect_MhsRequestBuilderIsCalledWithStructuredRecordPayload() {
        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(mhsRequestBuilder).buildSendEhrExtractCoreRequest(eq(PAYLOAD), anyString(), anyString(), anyString());
    }

    private void setupMhsClientWithSuccessfulResponse() {
        when(mhsClient.sendMessageToMHS(request)).thenReturn("Successful Mhs Outbound Request");
    }

    @Test
    public void When_NewExtractCoreTask_Expect_PlaceholdersWithinStructuredRecordPayloadAreReplaced() {
        setupMhsClientWithSuccessfulResponse();
        final var expectedPayload = "This is a payload. 123456";

        when(storageDataWrapper.getData()).thenReturn(PAYLOAD_WITH_LENGTH_PLACEHOLDER);

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(mhsRequestBuilder)
                .buildSendEhrExtractCoreRequest(eq(expectedPayload), anyString(), anyString(), anyString());
    }

    @Test
    public void When_NewExtractCoreTask_Expect_MhsRequestIsSentToMhsClient() {
        setupMhsClientWithSuccessfulResponse();

        var dummyRequestHeaderSpec = mock(RequestHeadersSpec.class);
        when(mhsRequestBuilder.buildSendEhrExtractCoreRequest(any(), any(), any(), any())).thenReturn(dummyRequestHeaderSpec);

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(mhsClient).sendMessageToMHS(dummyRequestHeaderSpec);
    }

    @Test
    public void When_NewExtractCoreTaskWithDocuments_Expect_PositiveAcknowledgementNotSent() {
        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(sendAcknowledgementTaskDispatcher, never()).sendPositiveAcknowledgement(ehrExtractStatus);
    }

    @Test
    public void When_NewExtractCoreTaskWithoutDocuments_Expect_PositiveAcknowledgementSent() {
        ehrExtractStatus.getGpcAccessDocument().setDocuments(List.of());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        ehrExtractStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).orElseThrow();
        verify(sendAcknowledgementTaskDispatcher, times(1)).sendPositiveAcknowledgement(ehrExtractStatus);
    }

    @Test
    public void When_ExtractCoreThrowsException_Expect_EhrExtractStatusNotUpdated() {

        doThrow(InvalidOutboundMessageException.class)
            .when(mhsRequestBuilder).buildSendEhrExtractCoreRequest(any(), any(), any(), any());

        assertThrows(InvalidOutboundMessageException.class,
            () -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId())
             .orElseThrow();

        assertThat(ehrExtractUpdated.getEhrExtractCore()).isNull();
    }

    @Test
    public void When_ExtractCoreThrowsMhsConnectionException_Expect_ExceptionThrownAndDbNotUpdated() {


        doThrow(MhsConnectionException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsConnectionException.class)
            .isThrownBy(() -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractStatusUpdated = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId()).orElseThrow();

        assertThat(ehrExtractStatusUpdated.getEhrExtractCore()).isNull();
    }

    @Test
    public void When_ExtractCoreThrowsMhsServerErrorException_Expect_ExceptionThrownAndDbNotUpdated() {

        doThrow(MhsServerErrorException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsServerErrorException.class)
            .isThrownBy(() -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractStatusUpdated = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId()).orElseThrow();

        assertThat(ehrExtractStatusUpdated.getEhrExtractCore()).isNull();
    }

    @BeforeEach
    public void prepareCommonStubbing() {
        ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(storageConnectorService.downloadFile(eq(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME))).thenReturn(storageDataWrapper);
        when(storageDataWrapper.getData()).thenReturn(PAYLOAD);
        when(sendEhrExtractCoreTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendEhrExtractCoreTaskDefinition.getTaskId()).thenReturn(randomIdGeneratorService.createNewId());
        when(sendEhrExtractCoreTaskDefinition.getEhrExtractMessageId()).thenReturn(randomIdGeneratorService.createNewId());
        when(sendEhrExtractCoreTaskDefinition.getFromOdsCode()).thenReturn(FROM_ODS_CODE);
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var ehrExtractCore = ehrExtractStatusUpdated.getEhrExtractCore();
        assertThat(ehrExtractCore.getSentAt()).isNotNull();
        assertThat(ehrExtractCore.getTaskId()).isNotNull();
    }
}
