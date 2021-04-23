package uk.nhs.adaptors.gp2gp.gpc;

import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorException;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GetGpcDocumentComponentTest extends BaseTaskTest {
    private static final String DOCUMENT_NAME = EhrStatusConstants.DOCUMENT_ID + ".json";
    private static final String INVALID_DOCUMENT_ID = "non-existing-id";
    private static final String NO_RECORD_FOUND = "NO_RECORD_FOUND";
    private static final String NO_RECORD_FOUND_STRING = "No Record Found";

    @Autowired
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;
    @MockBean
    private DetectTranslationCompleteService detectTranslationCompleteService;
    @Autowired
    private GpcConfiguration configuration;

    @Test
    public void When_NewAccessDocumentTaskIsStarted_Expect_DatabaseUpdatedAndDocumentAddedToObjectStore() throws IOException {
        var ehrExtractStatus = addEhrStatusToDatabase();
        var taskDefinition = buildValidAccessTask(ehrExtractStatus, EhrStatusConstants.DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus, ehrExtractStatus, taskDefinition);

        var inputStream = storageConnector.downloadFromStorage(DOCUMENT_NAME);
        var storageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);

        assertThat(storageDataWrapper.getConversationId()).isEqualTo(taskDefinition.getConversationId());
        assertThat(storageDataWrapper.getTaskId()).isEqualTo(taskDefinition.getTaskId());
        assertThat(storageDataWrapper.getType()).isEqualTo(taskDefinition.getTaskType().getTaskTypeHeaderValue());
        assertThat(storageDataWrapper.getData()).contains(EhrStatusConstants.DOCUMENT_ID);

        String messageId = updatedEhrExtractStatus.getGpcAccessDocument()
            .getDocuments()
            .get(0)
            .getMessageId();
        assertThat(storageDataWrapper.getData()).contains(messageId);

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus);
    }

    @Test
    public void When_NewAccessDocumentTaskIsStartedAndThenUpdated_Expect_DatabaseAndObjectStoreUpdated() throws IOException {
        var ehrExtractStatus = addEhrStatusToDatabase();
        var taskDefinition = buildValidAccessTask(ehrExtractStatus, EhrStatusConstants.DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus1 = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        var inputStream = storageConnector.downloadFromStorage(DOCUMENT_NAME);
        var storageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);

        var newTaskDefinition = buildValidAccessTask(ehrExtractStatus, EhrStatusConstants.DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(newTaskDefinition);

        var updatedEhrExtractStatus2 = ehrExtractStatusRepository.findByConversationId(newTaskDefinition.getConversationId()).get();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus2, updatedEhrExtractStatus1, newTaskDefinition);

        var updatedFileInputStream = storageConnector.downloadFromStorage(DOCUMENT_NAME);
        var updatedStorageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(updatedFileInputStream), StorageDataWrapper.class);

        assertThat(storageDataWrapper.getTaskId()).isNotEqualTo(updatedStorageDataWrapper.getTaskId());

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus2);
    }

    @Test
    public void When_AccessDocumentNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcDocumentTaskDefinition documentTaskDefinition = buildValidAccessTask(ehrExtractStatus, INVALID_DOCUMENT_ID);

        Exception exception = assertThrows(GpConnectException.class, () -> getGpcDocumentTaskExecutor.execute(documentTaskDefinition));
        assertOperationOutcome(exception);

        var ehrExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        var gpcDocuments = ehrExtract.getGpcAccessDocument().getDocuments();
        assertThat(gpcDocuments).hasSize(1);
        assertThat(gpcDocuments.get(0).getTaskId()).isNull();
        assertThat(gpcDocuments.get(0).getAccessedAt()).isNull();
        assertThat(gpcDocuments.get(0).getObjectName()).isNull();

        assertThrows(StorageConnectorException.class, () -> storageConnector.downloadFromStorage(DOCUMENT_NAME));

        verify(detectTranslationCompleteService, never()).beginSendingCompleteExtract(any());
    }

    private EhrExtractStatus addEhrStatusToDatabase() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
            .documents(prepareDocuments())
            .build());
        return ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    private GetGpcDocumentTaskDefinition buildValidAccessTask(EhrExtractStatus ehrExtractStatus, String documentId) {
        return GetGpcDocumentTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .documentId(documentId)
            .accessDocumentUrl(buildDocumentUrl(documentId))
            .build();
    }

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated,
        EhrExtractStatus ehrExtractStatus,
        GetGpcDocumentTaskDefinition taskDefinition) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());

        var gpcDocument = ehrExtractStatusUpdated.getGpcAccessDocument()
            .getDocuments()
            .get(0);
        assertThat(gpcDocument.getObjectName()).isEqualTo(DOCUMENT_NAME);
        assertThat(gpcDocument.getAccessedAt()).isNotNull();
        assertThat(gpcDocument.getTaskId()).isEqualTo(taskDefinition.getTaskId());
    }

    private List<EhrExtractStatus.GpcAccessDocument.GpcDocument> prepareDocuments() {
        return List.of(EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
            .documentId(EhrStatusConstants.DOCUMENT_ID)
            .accessDocumentUrl(EhrStatusConstants.GPC_ACCESS_DOCUMENT_URL)
            .build());
    }

    private void assertOperationOutcome(Exception exception) {
        var operationOutcomeString = exception.getMessage().replace("The following error occurred during GPC request: ", "");
        var operationOutcome = FHIR_PARSE_SERVICE.parseResource(operationOutcomeString, OperationOutcome.class).getIssueFirstRep();
        var coding = operationOutcome.getDetails().getCodingFirstRep();
        assertThat(coding.getCode()).isEqualTo(NO_RECORD_FOUND);
        assertThat(coding.getDisplay()).isEqualTo(NO_RECORD_FOUND_STRING);
    }

    private String buildDocumentUrl(String documentId) {
        return configuration.getUrl() + configuration.getDocumentEndpoint() + documentId;
    }
}
