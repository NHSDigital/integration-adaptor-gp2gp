package uk.nhs.adaptors.gp2gp.gpc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith({ SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class GetGpcDocumentComponentTest extends BaseTaskTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DOCUMENT_NAME = EhrStatusConstants.DOCUMENT_ID + ".json";
    private static final String INVALID_DOCUMENT_ID = "invalid-id";
    private static final String EXPECTED_ERROR_RESPONSE = "The following error occurred during Gpc Structured Request: "
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

    @Test
    public void When_NewAccessDocumentTaskIsStarted_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcDocumentTaskDefinition documentTaskDefinition = buildValidAccessTask(ehrExtractStatus, EhrStatusConstants.DOCUMENT_ID);
        getGpcDocumentTaskExecutor.execute(documentTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatAccessRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);

        var inputStream = storageConnector.downloadFromStorage(DOCUMENT_NAME);
        String storageDataWrapperString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        var storageDataWrapper = OBJECT_MAPPER.readValue(storageDataWrapperString, StorageDataWrapper.class);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, documentTaskDefinition);
    }

    @Test
    public void When_AccessDocumentNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcDocumentTaskDefinition documentTaskDefinition = buildValidAccessTask(ehrExtractStatus, INVALID_DOCUMENT_ID);

        Exception exception = assertThrows(GpConnectException.class, () -> getGpcDocumentTaskExecutor.execute(documentTaskDefinition));
        assertThat(exception.getMessage(), is(EXPECTED_ERROR_RESPONSE));

        var ehrExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtract.getGpcAccessDocuments().size(), is(1));
        assertThat(ehrExtract.getGpcAccessDocuments().get(0).getTaskId(), is(nullValue()));
        assertThat(ehrExtract.getGpcAccessDocuments().get(0).getAccessedAt(), is(nullValue()));

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

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt(), not(ehrExtractStatus.getUpdatedAt()));

        var gpcAccessDocument = ehrExtractStatusUpdated.getGpcAccessDocuments().get(0);
        assertThat(gpcAccessDocument.getObjectName(), is(DOCUMENT_NAME));
        assertThat(gpcAccessDocument.getAccessedAt(), is(notNullValue()));
        assertThat(gpcAccessDocument.getTaskId(), is(notNullValue()));
    }

    private void assertThatObjectCreated(StorageDataWrapper storageDataWrapper, EhrExtractStatus ehrExtractStatus,
            GetGpcDocumentTaskDefinition documentTaskDefinition) {
        assertThat(storageDataWrapper.getConversationId(), is(ehrExtractStatus.getConversationId()));
        assertThat(storageDataWrapper.getTaskId(), is(ehrExtractStatus.getGpcAccessDocuments().get(0).getTaskId()));
        assertThat(storageDataWrapper.getType(), is(documentTaskDefinition.getTaskType().getTaskTypeHeaderValue()));
        assertThat(storageDataWrapper.getResponse(), is(notNullValue()));
    }
}
