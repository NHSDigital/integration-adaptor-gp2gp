package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import uk.nhs.adaptors.gp2gp.common.storage.LocalMockConnector;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class SendDocumentComponentTest {
    private static final String DOCUMENT_NAME = "some-conversation-id/document-name.json";

    @Autowired
    private SendDocumentTaskExecutor sendDocumentTaskExecutor;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private LocalMockConnector localMockConnector;

    @Test
    public void When_SendDocumentTaskRunsTwice_Expect_DatabaseOverwritesEhrExtractStatus() throws IOException {
        var inputStream = readMessageAsInputStream();
        localMockConnector.uploadToStorage(inputStream, inputStream.available(), DOCUMENT_NAME);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var ehrRequest = ehrExtractStatus.getEhrRequest();
        var sendDocumentTaskDefinition = SendDocumentTaskDefinition.builder()
            .documentName(DOCUMENT_NAME)
            .conversationId(ehrExtractStatus.getConversationId())
            .fromOdsCode(ehrRequest.getFromOdsCode())
            .taskId(ehrRequest.getFromOdsCode())
            .messageId(ehrRequest.getMessageId())
            .build();

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractFirst = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        sendDocumentTaskExecutor.execute(sendDocumentTaskDefinition);
        var ehrExtractSecond = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(ehrExtractFirst.getUpdatedAt()).isNotEqualTo(ehrExtractSecond.getUpdatedAt());
    }

    private InputStream readMessageAsInputStream() throws IOException {
        File file = new File("src/intTest/resources/outboundMessage.json");
        return new FileInputStream(file);
    }
}
