package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import uk.nhs.adaptors.gp2gp.common.storage.LocalMockConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsServerErrorException;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class SendDocumentComponentTest {
    private static final String DOCUMENT_NAME = "some-conversation-id/document-name.json";
    public static final String OUTBOUND_MESSAGE_JSON = "src/intTest/resources/outboundMessage.json";

    @MockBean
    private MhsClient mhsClient;

    @Autowired
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private LocalMockConnector localMockConnector;

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void When_SendDocumentTaskRunsWithOver20MbPayloadMessage_Expect_NoException() throws IOException {
        InputStream inputStream = createInputStreamWithAttachmentOfSize(22 * 1024 * 1024);
        localMockConnector.uploadToStorage(inputStream, inputStream.available(), DOCUMENT_NAME);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var sendDocumentTaskDefinition = prepareTaskDefinition(ehrExtractStatus);

        assertDoesNotThrow(() -> sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition));
    }

    private @NotNull InputStream createInputStreamWithAttachmentOfSize(int sizeOfAttachmentInBytes) throws IOException {
        var inputStream = readMessageAsInputStream();
        ObjectMapper mapper = new ObjectMapper();
        StorageDataWrapper wrapper = mapper.readValue(inputStream, StorageDataWrapper.class);
        OutboundMessage outboundMessage = mapper.readValue(wrapper.getData(), OutboundMessage.class);
        outboundMessage.getAttachments().getFirst().setPayload("1".repeat(sizeOfAttachmentInBytes));
        wrapper.setData(mapper.writeValueAsString(outboundMessage));

        return IOUtils.toInputStream(mapper.writeValueAsString(wrapper), "UTF-8");
    }

    @Test
    public void When_SendDocumentTaskRunsTwice_Expect_DatabaseOverwritesEhrExtractStatus() throws IOException {
        var inputStream = readMessageAsInputStream();
        localMockConnector.uploadToStorage(inputStream, inputStream.available(), DOCUMENT_NAME);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var sendDocumentTaskDefinition = prepareTaskDefinition(ehrExtractStatus);

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractFirst = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractSecond = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(ehrExtractFirst.getUpdatedAt()).isNotEqualTo(ehrExtractSecond.getUpdatedAt());
    }

    @Test
    public void When_SendDocumentTask_WithMhsConnectionException_Expect_ExceptionThrownAndDatabaseNotUpdated() throws IOException {
        var inputStream = readMessageAsInputStream();
        localMockConnector.uploadToStorage(inputStream, inputStream.available(), DOCUMENT_NAME);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var sendDocumentTaskDefinition = prepareTaskDefinition(ehrExtractStatus);

        doThrow(MhsConnectionException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsConnectionException.class)
            .isThrownBy(() -> sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition));

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId())
            .orElseThrow();

        assertThat(ehrExtractStatusHasSentDocuments(ehrExtractUpdated)).isFalse();
    }

    @Test
    public void When_SendDocumentTask_WithMhsServerFailureException_Expect_ExceptionThrownAndDataBaseNotUpdated() throws IOException {
        var inputStream = readMessageAsInputStream();
        localMockConnector.uploadToStorage(inputStream, inputStream.available(), DOCUMENT_NAME);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var sendDocumentTaskDefinition = prepareTaskDefinition(ehrExtractStatus);

        doThrow(MhsServerErrorException.class).when(mhsClient).sendMessageToMHS(any());

        assertThatExceptionOfType(MhsServerErrorException.class)
            .isThrownBy(() -> sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition));

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId())
            .orElseThrow();

        assertThat(ehrExtractStatusHasSentDocuments(ehrExtractUpdated)).isFalse();
    }

    private InputStream readMessageAsInputStream() throws IOException {
        File file = new File(OUTBOUND_MESSAGE_JSON);
        return new FileInputStream(file);
    }

    private SendDocumentTaskDefinition prepareTaskDefinition(EhrExtractStatus ehrExtractStatus) {
        var ehrRequest = ehrExtractStatus.getEhrRequest();
        return SendDocumentTaskDefinition.builder()
            .documentName(DOCUMENT_NAME)
            .conversationId(ehrExtractStatus.getConversationId())
            .fromOdsCode(ehrRequest.getFromOdsCode())
            .taskId(ehrRequest.getFromOdsCode())
            .messageId(ehrRequest.getMessageId())
            .build();
    }

    private boolean ehrExtractStatusHasSentDocuments(EhrExtractStatus ehrExtractStatus) {
        return ehrExtractStatus.getGpcAccessDocument().getDocuments().stream()
            .anyMatch(gpcDocument -> gpcDocument.getSentToMhs() != null);
    }
}
