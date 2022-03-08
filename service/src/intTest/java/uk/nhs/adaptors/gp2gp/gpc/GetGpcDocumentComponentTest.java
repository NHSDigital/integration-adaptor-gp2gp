package uk.nhs.adaptors.gp2gp.gpc;

import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorException;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GetGpcDocumentComponentTest extends BaseTaskTest {
    private static final String NO_RECORD_FOUND = "NO_RECORD_FOUND";
    private static final String NO_RECORD_FOUND_STRING = "No Record Found";
    private static final String ODS_CODE_PLACEHOLDER = "@ODS_CODE@";
    private static final String EXPECTED_DOCUMENT_JSON_FILENAME =
        CONVERSATION_ID.concat("/").concat(DOCUMENT_ID).concat(".json");
    private static final String DOCUMENT_BINARY_ENDPOINT = "/documents/fhir/Binary/";

    @Autowired
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private StorageConnector storageConnector;

    @SpyBean
    private StorageConnectorService storageConnectorService;

    @MockBean
    private DetectTranslationCompleteService detectTranslationCompleteService;

    @Autowired
    private GpcConfiguration configuration;

    @Test
    public void When_NewAccessDocumentTaskIsStarted_Expect_DatabaseUpdatedAndDocumentAddedToObjectStore() throws IOException {
        var ehrExtractStatus = addEhrStatusToDatabase();
        var taskDefinition = buildValidAccessTask(ehrExtractStatus, DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository
            .findByConversationId(taskDefinition.getConversationId())
            .orElseThrow();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus, ehrExtractStatus, taskDefinition);

        try (var inputStream = storageConnector.downloadFromStorage(EXPECTED_DOCUMENT_JSON_FILENAME)) {
            var storageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);

            assertThat(storageDataWrapper.getConversationId()).isEqualTo(taskDefinition.getConversationId());
            assertThat(storageDataWrapper.getTaskId()).isEqualTo(taskDefinition.getTaskId());
            assertThat(storageDataWrapper.getType()).isEqualTo(taskDefinition.getTaskType().getTaskName());
            assertThat(storageDataWrapper.getData()).contains(DOCUMENT_ID);

            String messageId = updatedEhrExtractStatus.getGpcAccessDocument()
                .getDocuments()
                .get(0)
                .getMessageId();
            assertThat(storageDataWrapper.getData()).contains(messageId);

            verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus);
            verify(storageConnectorService).uploadFile(
                any(),
                eq(EXPECTED_DOCUMENT_JSON_FILENAME)
            );
        }
    }

    @Test
    public void When_NewAccessDocumentTaskIsStartedAndThenUpdated_Expect_DatabaseAndObjectStoreUpdated() throws IOException {
        var ehrExtractStatus = addEhrStatusToDatabase();
        var taskDefinition = buildValidAccessTask(ehrExtractStatus, DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus1 = ehrExtractStatusRepository
            .findByConversationId(taskDefinition.getConversationId())
            .orElseThrow();

        try (var inputStream = storageConnector.downloadFromStorage(EXPECTED_DOCUMENT_JSON_FILENAME)) {
            var storageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);

            var newTaskDefinition = buildValidAccessTask(ehrExtractStatus, DOCUMENT_ID);
            getGpcDocumentTaskExecutor.execute(newTaskDefinition);

            var updatedEhrExtractStatus2 = ehrExtractStatusRepository
                .findByConversationId(newTaskDefinition.getConversationId())
                .orElseThrow();
            assertThatAccessRecordWasUpdated(updatedEhrExtractStatus2, updatedEhrExtractStatus1, newTaskDefinition);

            var updatedFileInputStream = storageConnector.downloadFromStorage(EXPECTED_DOCUMENT_JSON_FILENAME);
            var updatedStorageDataWrapper =
                OBJECT_MAPPER.readValue(new InputStreamReader(updatedFileInputStream), StorageDataWrapper.class);

            assertThat(storageDataWrapper.getTaskId()).isNotEqualTo(updatedStorageDataWrapper.getTaskId());

            verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus2);
            verify(storageConnectorService, times(2)).uploadFile(
                any(),
                eq(EXPECTED_DOCUMENT_JSON_FILENAME)
            );
        }
    }

    @Test
    public void When_AccessDocumentNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var conversationId = UUID.randomUUID().toString();
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setConversationId(conversationId);
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcDocumentTaskDefinition documentTaskDefinition = buildValidAccessTask(ehrExtractStatus, "non-existing-id");

        Exception exception = assertThrows(GpConnectException.class, () -> getGpcDocumentTaskExecutor.execute(documentTaskDefinition));
        assertOperationOutcome(exception);

        var gpcDocuments = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId())
            .map(x -> x.getGpcAccessDocument().getDocuments())
            .orElseThrow();
        assertThat(gpcDocuments).hasSize(1);
        assertThat(gpcDocuments.get(0).getTaskId()).isNull();
        assertThat(gpcDocuments.get(0).getAccessedAt()).isNull();
        assertThat(gpcDocuments.get(0).getObjectName()).isNull();

        String documentJsonFilename = conversationId + "/non-existing-id.json";
        assertThrows(StorageConnectorException.class, () -> storageConnector.downloadFromStorage(documentJsonFilename));

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
            .messageId(documentId)
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .documentId(documentId)
            .accessDocumentUrl(buildDocumentUrl(documentId, ehrExtractStatus.getEhrRequest().getToOdsCode()))
            .build();
    }

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated,
        EhrExtractStatus ehrExtractStatus,
        GetGpcDocumentTaskDefinition taskDefinition) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());

        var gpcDocument = ehrExtractStatusUpdated.getGpcAccessDocument()
            .getDocuments()
            .get(0);
        assertThat(gpcDocument.getObjectName()).isEqualTo(EXPECTED_DOCUMENT_JSON_FILENAME);
        assertThat(gpcDocument.getAccessedAt()).isNotNull();
        assertThat(gpcDocument.getTaskId()).isEqualTo(taskDefinition.getTaskId());
    }

    private List<EhrExtractStatus.GpcDocument> prepareDocuments() {
        return List.of(EhrExtractStatus.GpcDocument.builder()
            .documentId(DOCUMENT_ID)
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

    private String buildDocumentUrl(String documentId, String odsCode) {
        return configuration.getUrl().replace(ODS_CODE_PLACEHOLDER, odsCode) + DOCUMENT_BINARY_ENDPOINT + documentId;
    }
}
