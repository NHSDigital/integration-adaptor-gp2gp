package uk.nhs.adaptors.gp2gp.gpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorException;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GetGpcDocumentComponentTest extends BaseTaskTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DOCUMENT_NAME = EhrStatusConstants.DOCUMENT_ID + ".json";
    private static final String INVALID_DOCUMENT_ID = "invalid-id";
    private static final String EXPECTED_ERROR_RESPONSE = "The following error occurred during Gpc Request: "
        + "{\n  \"resourceType\": \"OperationOutcome\",\n  \"meta\": {\n    "
        + "\"profile\": [ \"https://fhir.nhs.uk/StructureDefinition/gpconnect-operationoutcome-1\" ]\n  },\n  "
        + "\"issue\": [ {\n    \"severity\": \"error\",\n    "
        + "\"code\": \"invalid\",\n    \"details\": {\n      \"coding\": [ {\n        "
        + "\"system\": \"https://fhir.nhs.uk/ValueSet/gpconnect-error-or-warning-code-1\",\n        "
        + "\"code\": \"NO_RECORD_FOUND\",\n        \"display\": \"No Record Found\"\n      } ]\n    },\n    "
        + "\"diagnostics\": \"No record found\"\n  } ]\n}";

    @Autowired
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;

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
    public void When_NewAccessDocumentTaskIsStarted_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = setupDatabase();
        var taskDefinition = buildValidAccessTask(ehrExtractStatus, EhrStatusConstants.DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus, ehrExtractStatus, taskDefinition);

        var inputStream = storageConnector.downloadFromStorage(DOCUMENT_NAME);
        var storageDataWrapper = OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);

        assertThat(storageDataWrapper.getConversationId()).isEqualTo(taskDefinition.getConversationId());
        assertThat(storageDataWrapper.getTaskId()).isEqualTo(taskDefinition.getTaskId());
        assertThat(storageDataWrapper.getType()).isEqualTo(taskDefinition.getTaskType().getTaskTypeHeaderValue());
        assertThat(storageDataWrapper.getResponse()).contains(EhrStatusConstants.DOCUMENT_ID);
    }

    @Test
    public void When_AccessDocumentNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcDocumentTaskDefinition documentTaskDefinition = buildValidAccessTask(ehrExtractStatus, INVALID_DOCUMENT_ID);

        Exception exception = assertThrows(GpConnectException.class, () -> getGpcDocumentTaskExecutor.execute(documentTaskDefinition));
        assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_RESPONSE);

        var ehrExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        var gpcDocuments = ehrExtract.getGpcAccessDocument().getDocuments();
        assertThat(gpcDocuments).hasSize(1);
        assertThat(gpcDocuments.get(0).getTaskId()).isNull();
        assertThat(gpcDocuments.get(0).getAccessedAt()).isNull();
        assertThat(gpcDocuments.get(0).getObjectName()).isNull();

        assertThrows(StorageConnectorException.class, () -> storageConnector.downloadFromStorage(DOCUMENT_NAME));
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
            .build();
    }

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated,
                                                  EhrExtractStatus ehrExtractStatus,
                                                  GetGpcDocumentTaskDefinition taskDefinition) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());

        var gpcDocument =
            ehrExtractStatusUpdated.getGpcAccessDocument().getDocuments().get(0);
        assertThat(gpcDocument.getObjectName()).isEqualTo(DOCUMENT_NAME);
        assertThat(gpcDocument.getAccessedAt()).isNotNull();
        assertThat(gpcDocument.getTaskId()).isEqualTo(taskDefinition.getTaskId());
    }
}
