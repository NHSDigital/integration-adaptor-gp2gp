package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GpcFindDocumentsComponentTest extends BaseTaskTest {
    private static final String NHS_NUMBER_WITH_DOCUMENT = "9690937286";
    private static final String NHS_NUMBER_WITHOUT_DOCUMENT = "9690937294";
    private static final String VALID_DOCUMENT_URL = "https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/documents/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510";

    @Autowired
    private GpcFindDocumentsTaskExecutor gpcFindDocumentsTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    private EhrExtractStatus setupDatabase() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
            .documents(List.of(
                EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
                    .documentId(EhrStatusConstants.DOCUMENT_ID)
                    .accessDocumentUrl(EhrStatusConstants.GPC_ACCESS_DOCUMENT_URL)
                    .build()
            ))
            .build());
        return ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    @Test
    public void When_FindDocumentTaskIsStartedForPatientWithDocument_Expect_DatabaseToBeUpdated() throws IOException {
        var ehrExtractStatus = setupDatabase();
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, NHS_NUMBER_WITH_DOCUMENT);
        gpcFindDocumentsTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus, ehrExtractStatus, taskDefinition);
        Mockito.verify(taskDispatcher).createTask(buildGetDocumentTask(taskDefinition));

    }

    @Test
    public void When_FindDocumentTaskIsStartedForPatientWithoutDocument_Expect_DatabaseToNotBeUpdated() {
        var ehrExtractStatus = setupDatabase();
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, NHS_NUMBER_WITHOUT_DOCUMENT);
        gpcFindDocumentsTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThat(updatedEhrExtractStatus.getGpcAccessDocument().getDocuments().size()).isEqualTo(0);

    }

    private static GpcFindDocumentsTaskDefinition buildFindDocumentTask(EhrExtractStatus ehrExtractStatus, String nhsNumber) {
        return GpcFindDocumentsTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .nhsNumber(nhsNumber)
            .build();
    }

    private static GetGpcDocumentTaskDefinition buildGetDocumentTask(GpcFindDocumentsTaskDefinition taskDefinition) {
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(GetGpcDocumentTaskDefinition.extractIdFromUrl(VALID_DOCUMENT_URL))
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(VALID_DOCUMENT_URL)
            .build();
    }

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated,
        EhrExtractStatus ehrExtractStatus,
        GpcFindDocumentsTaskDefinition taskDefinition) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());

        assertThat(ehrExtractStatusUpdated.getGpcAccessDocument().getDocuments().size()).isEqualTo(1);
        var gpcDocument =
            ehrExtractStatusUpdated.getGpcAccessDocument().getDocuments().get(0);

        assertThat(gpcDocument).isNotNull();
        assertThat(gpcDocument.getAccessDocumentUrl()).isEqualTo(VALID_DOCUMENT_URL);
        assertThat(gpcDocument.getAccessedAt()).isNotNull();
        assertThat(gpcDocument.getTaskId()).isEqualTo(taskDefinition.getTaskId());
    }

}
