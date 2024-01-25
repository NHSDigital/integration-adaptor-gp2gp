package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GetAbsentAttachmentComponentTest extends BaseTaskTest {
    @Autowired
    private GetAbsentAttachmentTaskExecutor getAbsentAttachmentTaskExecutor;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_AbsentAttachmentTaskExecuted_Expect_DocumentRecordUpdatedWithErrorReason() {
        var ehrExtractStatus = addEhrStatusToDatabase();
        var taskDefinition = GetAbsentAttachmentTaskDefinition.builder()
                .messageId(DOCUMENT_ID)
                .title("This is the reason why the document won't be sent over GP2GP.")
                .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
                .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
                .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
                .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
                .conversationId(ehrExtractStatus.getConversationId())
                .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
                .taskId(UUID.randomUUID().toString())
                .documentId(DOCUMENT_ID)
                .build();
        getAbsentAttachmentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository
            .findByConversationId(taskDefinition.getConversationId())
            .orElseThrow();

        assertThat(updatedEhrExtractStatus
                .getGpcAccessDocument()
                .getDocuments()
                .get(0)
                .getGpConnectErrorMessage())
            .isEqualTo("This is the reason why the document won't be sent over GP2GP.");
    }

    private EhrExtractStatus addEhrStatusToDatabase() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
                .documents(List.of(EhrExtractStatus.GpcDocument.builder()
                        .documentId(DOCUMENT_ID)
                        .build()))
                .build());
        return ehrExtractStatusRepository.save(ehrExtractStatus);
    }

}
