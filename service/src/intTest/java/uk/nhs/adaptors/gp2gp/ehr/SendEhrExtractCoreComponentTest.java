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


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskExecutor;
import uk.nhs.adaptors.gp2gp.gpc.GpcFilenameUtils;
import uk.nhs.adaptors.gp2gp.gpc.StructuredRecordMappingService;
import uk.nhs.adaptors.gp2gp.mhs.InvalidOutboundMessageException;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendEhrExtractCoreComponentTest extends BaseTaskTest {
    private static final String OUTBOUND_MESSAGE = serializeOutboundMessage("payload");
    public static final String OUTBOUND_MESSAGE_WITH_PLACEHOLDER =
            serializeOutboundMessage("payload ${LENGTH_PLACEHOLDER_ID=" + DOCUMENT_ID + "}");

    private static final String EXPECTED_STRUCTURED_RECORD_JSON_FILENAME =
        CONVERSATION_ID.concat("/").concat(CONVERSATION_ID).concat("_gpc_structured.json");
    public static final String TWO_BYTE_CHARACTER = "£";
    public static final String SEVENTEEN_BYTE_PAYLOAD = "REALLY REALLY BI" + TWO_BYTE_CHARACTER;
    public static final String TEXT_XML_CONTENT_TYPE = "text/xml";
    public static final String COMPRESSED_EHR_EXTRACT_DOCUMENT_ID = "4";
    public static final String COMPRESSED_EHR_EXTRACT_MESSAGE_ID = "5";
    public static final String COMPRESSED_EHR_EXTRACT_TASK_ID = "6";
    public static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");
    public static final String COMPRESSED_LARGE_PAYLOAD = "H4sIAAAAAAAA/wtydfTxiVQIglBOnocWAwA3VEfTEgAAAA==";

    @MockBean
    private EhrDocumentMapper ehrDocumentMapper;

    @MockBean
    private TimestampService timestampService;

    @MockBean
    private RandomIdGeneratorService randomIdGeneratorService;

    @Mock
    private StorageDataWrapper storageDataWrapper;

    @MockBean
    private WebClient.RequestHeadersSpec<?> request;

    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;

    @MockBean
    private MhsClient mhsClient;

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

    @MockBean
    private StructuredRecordMappingService structuredRecordMappingService;

    @Test
    public void When_NewExtractCoreTask_Expect_DatabaseUpdated() {
        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        assertThatInitialRecordWasUpdated(reloadEhrStatus(), ehrExtractStatus);
    }

    @Test
    public void When_NewExtractCoreTask_Expect_MhsRequestBuilderIsCalledWithStructuredRecordPayload() {
        setupMhsClientWithSuccessfulResponse();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(mhsRequestBuilder).buildSendEhrExtractCoreRequest(eq(OUTBOUND_MESSAGE), anyString(), anyString(), anyString());
    }

    private void setupMhsClientWithSuccessfulResponse() {
        when(mhsClient.sendMessageToMHS(request)).thenReturn("Successful Mhs Outbound Request");
    }

    @Test
    public void When_NewExtractCoreTask_Expect_PlaceholdersWithinStructuredRecordPayloadAreReplaced() {
        setupMhsClientWithSuccessfulResponse();
        final var expectedPayload = serializeOutboundMessage("payload 123456");

        when(storageDataWrapper.getData()).thenReturn(OUTBOUND_MESSAGE_WITH_PLACEHOLDER);

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(mhsRequestBuilder)
                .buildSendEhrExtractCoreRequest(eq(expectedPayload), anyString(), anyString(), anyString());
    }

    @Test
    public void When_NewExtractCoreTask_Expect_MhsRequestIsSentToMhsClient() {
        setupMhsClientWithSuccessfulResponse();

        var dummyRequestHeaderSpec = mock(RequestHeadersSpec.class);
        when(mhsRequestBuilder.buildSendEhrExtractCoreRequest(any(), any(), any(), any()))
            .thenReturn(dummyRequestHeaderSpec);

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

        verify(sendAcknowledgementTaskDispatcher, times(1)).sendPositiveAcknowledgement(reloadEhrStatus());
    }

    @Test
    public void When_ExtractCoreWithLargeMessage_Expect_MhsRequestBuilderCalledWithSkeletonMessage() {
        setupMhsClientWithSuccessfulResponse();
        setupEhrExtractAsLargeMessage();
        when(structuredRecordMappingService.buildSkeletonEhrExtractXml(SEVENTEEN_BYTE_PAYLOAD, COMPRESSED_EHR_EXTRACT_DOCUMENT_ID))
                .thenReturn("ASkeleton");

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        assertThat(getOutboundMessagePassedToRequestBuilder().getPayload()).isEqualTo("ASkeleton");
    }

    @SneakyThrows
    @Test
    public void When_ExtractCoreWithLargeMessage_Expect_NewGpcDocumentInDatabase() {
        setupMhsClientWithSuccessfulResponse();
        setupEhrExtractAsLargeMessage();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        var fileName = GpcFilenameUtils.generateLargeExrExtractFilename(COMPRESSED_EHR_EXTRACT_DOCUMENT_ID);
        assertThat(reloadEhrStatus().getGpcAccessDocument().getDocuments().get(1))
            .isEqualTo(EhrExtractStatus.GpcDocument.builder()
                .documentId(COMPRESSED_EHR_EXTRACT_DOCUMENT_ID)
                .accessDocumentUrl(null)
                .contentType(TEXT_XML_CONTENT_TYPE)
                .objectName(fileName)
                .fileName(fileName)
                .accessedAt(NOW)
                .taskId(COMPRESSED_EHR_EXTRACT_TASK_ID)
                .messageId(COMPRESSED_EHR_EXTRACT_MESSAGE_ID)
                .isSkeleton(true)
                .identifier(null)
                .build());
    }

    @Test
    public void When_ExtractCoreWithLargeMessage_Expect_MhsRequestBuilderCalledWithAdditionalExternalSkeletonAttachment() {
        setupMhsClientWithSuccessfulResponse();
        setupEhrExtractAsLargeMessage();

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        assertThat(getOutboundMessagePassedToRequestBuilder().getExternalAttachments()).hasSize(1);
        assertThat(getOutboundMessagePassedToRequestBuilder().getExternalAttachments().get(0))
            .usingRecursiveComparison()
            .isEqualTo(
                OutboundMessage.ExternalAttachment.builder()
                    .documentId("_" + COMPRESSED_EHR_EXTRACT_DOCUMENT_ID)
                    .messageId(COMPRESSED_EHR_EXTRACT_MESSAGE_ID)
                    .description(
                        OutboundMessage.AttachmentDescription.builder()
                            .fileName(GpcFilenameUtils.generateLargeExrExtractFilename(COMPRESSED_EHR_EXTRACT_DOCUMENT_ID))
                            .contentType("text/xml")
                            .length(COMPRESSED_LARGE_PAYLOAD.length())
                            .compressed(true)
                            .largeAttachment(true)
                            .originalBase64(false)
                            .domainData(GetGpcStructuredTaskExecutor.SKELETON_ATTACHMENT)
                            .build()
                            .toString()
                    ).build()
            );
    }

    @Test
    @SneakyThrows
    public void When_ExtractCoreWithLargeMessage_Expect_CompressedEhrExtractUploadedToStorageAsStorageDataWrapper() {
        setupMhsClientWithSuccessfulResponse();
        setupEhrExtractAsLargeMessage();

        when(ehrDocumentMapper.generateMhsPayload(
            sendEhrExtractCoreTaskDefinition,
            COMPRESSED_EHR_EXTRACT_MESSAGE_ID,
            COMPRESSED_EHR_EXTRACT_DOCUMENT_ID,
            "application/xml"
        )).thenReturn("<COPC />");

        sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition);

        verify(storageConnectorService).uploadFile(
            StorageDataWrapper.builder()
                .type("uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition")
                .conversationId(CONVERSATION_ID)
                .taskId(COMPRESSED_EHR_EXTRACT_TASK_ID)
                .data(new ObjectMapper().writeValueAsString(
                    OutboundMessage.builder()
                        .payload("<COPC />")
                        .attachments(List.of(OutboundMessage.Attachment.builder()
                            .contentType("text/xml")
                            .isBase64(true)
                            .description(COMPRESSED_EHR_EXTRACT_DOCUMENT_ID)
                            .payload(COMPRESSED_LARGE_PAYLOAD)
                            .build()
                        )).build()
                )).build(),
            GpcFilenameUtils.generateLargeExrExtractFilename(COMPRESSED_EHR_EXTRACT_DOCUMENT_ID));
    }

    private @NotNull EhrExtractStatus reloadEhrStatus() {
        return ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).orElseThrow();
    }

    private void setupEhrExtractAsLargeMessage() {
        when(storageDataWrapper.getData()).thenReturn(serializeOutboundMessage(SEVENTEEN_BYTE_PAYLOAD));
    }

    @SneakyThrows
    private OutboundMessage getOutboundMessagePassedToRequestBuilder() {
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mhsRequestBuilder).buildSendEhrExtractCoreRequest(captor.capture(), anyString(), anyString(), anyString());
        return new ObjectMapper().readValue(captor.getValue(), OutboundMessage.class);
    }

    @SneakyThrows
    private static String serializeOutboundMessage(String payload) {
        return new ObjectMapper().writeValueAsString(OutboundMessage.builder()
            .payload(payload)
            .attachments(Collections.emptyList())
            .externalAttachments(Collections.emptyList())
            .build()
        );
    }

    @Test
    public void When_ExtractCoreThrowsException_Expect_EhrExtractStatusNotUpdated() {

        doThrow(InvalidOutboundMessageException.class)
            .when(mhsRequestBuilder).buildSendEhrExtractCoreRequest(any(), any(), any(), any());

        assertThrows(InvalidOutboundMessageException.class,
            () -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractUpdated = reloadEhrStatus();

        assertThat(ehrExtractUpdated.getEhrExtractCore()).isNull();
    }

    @Test
    public void When_ExtractCoreThrowsMhsConnectionException_Expect_ExceptionThrownAndDbNotUpdated() {


        doThrow(MhsConnectionException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsConnectionException.class)
            .isThrownBy(() -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractStatusUpdated = reloadEhrStatus();

        assertThat(ehrExtractStatusUpdated.getEhrExtractCore()).isNull();
    }

    @Test
    public void When_ExtractCoreThrowsMhsServerErrorException_Expect_ExceptionThrownAndDbNotUpdated() {

        doThrow(MhsServerErrorException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsServerErrorException.class)
            .isThrownBy(() -> sendEhrExtractCoreTaskExecutor.execute(sendEhrExtractCoreTaskDefinition));

        var ehrExtractStatusUpdated = reloadEhrStatus();

        assertThat(ehrExtractStatusUpdated.getEhrExtractCore()).isNull();
    }

    @BeforeEach
    public void prepareCommonStubbing() {
        ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        when(timestampService.now()).thenReturn(NOW);
        when(randomIdGeneratorService.createNewId()).thenReturn(
                COMPRESSED_EHR_EXTRACT_DOCUMENT_ID,
                COMPRESSED_EHR_EXTRACT_MESSAGE_ID,
                COMPRESSED_EHR_EXTRACT_TASK_ID
        );
        when(storageConnectorService.downloadFile(eq(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME))).thenReturn(storageDataWrapper);
        when(storageDataWrapper.getData()).thenReturn(OUTBOUND_MESSAGE);
        sendEhrExtractCoreTaskDefinition = SendEhrExtractCoreTaskDefinition.builder()
                .conversationId(ehrExtractStatus.getConversationId())
                .taskId("123-456")
                .ehrExtractMessageId("789-123")
                .fromOdsCode(FROM_ODS_CODE)
                .build();
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var ehrExtractCore = ehrExtractStatusUpdated.getEhrExtractCore();
        assertThat(ehrExtractCore.getSentAt()).isNotNull();
        assertThat(ehrExtractCore.getTaskId()).isNotNull();
    }
}
