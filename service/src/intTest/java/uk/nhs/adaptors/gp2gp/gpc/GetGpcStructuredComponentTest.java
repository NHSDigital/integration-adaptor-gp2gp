package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
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
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class GetGpcStructuredComponentTest extends BaseTaskTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EXPECTED_ERROR_RESPONSE = "The following error occurred during Gpc Structured Request: "
        + "{\"resourceType\":\"OperationOutcome\",\"meta\":{\"profile\":[\"https://fhir.nhs"
        + ".uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1\"]},\"issue\":[{\"severity\":\"error\",\"code\":\"value\","
        + "\"details\":{\"coding\":[{\"system\":\"https://fhir.nhs.uk/STU3/CodeSystem/Spine-ErrorOrWarningCode-1\",\"code\":"
        + "\"INVALID_NHS_NUMBER\",\"display\":\"INVALID_NHS_NUMBER\"}]},\"diagnostics\":\"Invalid NHS number submitted: 77777\"}]}";
    private static final String TASK_ID = "032a60c7-4960-45eb-9b65-0e778c3da56b";
    public static final String STRUCTURED_RECORD_FHIR_PROFILE = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-StructuredRecord-Bundle-1";

    @Autowired
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;

    @Test
    public void When_NewStructuredTask_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated.getGpcAccessStructured().getObjectName());
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition);
    }

    @Test
    @SneakyThrows
    public void When_StructuredTaskRunTwice_Expect_ObjectToBeOverwrittenInStroage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = buildValidStructuredTask(ehrExtractStatus);

        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition);
        var ehrExtractStatus1 = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        var storageDataWrapper1 = getStorageDataWrapper(ehrExtractStatus1.getGpcAccessStructured().getObjectName());

        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition);
        var ehrExtractStatus2 = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        var storageDataWrapper2 = getStorageDataWrapper(ehrExtractStatus2.getGpcAccessStructured().getObjectName());

        assertThatObjectCreated(storageDataWrapper1, ehrExtractStatus1, structuredTaskDefinition);
        assertThatObjectCreated(storageDataWrapper2, ehrExtractStatus2, structuredTaskDefinition);

        assertThat(ehrExtractStatus1.getGpcAccessStructured().getAccessedAt())
            .isNotEqualTo(ehrExtractStatus2.getGpcAccessStructured().getAccessedAt());

        //        Need to use wiremocks for the full assertion to work - the public demonstrator payload differs between requests
//        assertThat(storageDataWrapper1).isEqualTo(storageDataWrapper2);
        assertThat(storageDataWrapper1)
            .usingRecursiveComparison().ignoringFields("response").isEqualTo(storageDataWrapper2);
    }

    @Test
    public void When_StructuredTaskPatientNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildInvalidNHSNumberStructuredTask(ehrExtractStatus);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1);
        });
        assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_RESPONSE);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtractUpdated.getGpcAccessStructured()).isNull();

        assertThrows(StorageConnectorException.class, () -> {
            storageConnector.downloadFromStorage(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        });
    }


    private GetGpcStructuredTaskDefinition buildValidStructuredTask(EhrExtractStatus ehrExtractStatus) {
        return GetGpcStructuredTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(TASK_ID)
            .build();
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var gpcAccessStructured = ehrExtractStatusUpdated.getGpcAccessStructured();
        assertThat(gpcAccessStructured.getObjectName()).isEqualTo(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        assertThat(gpcAccessStructured.getAccessedAt()).isNotNull();
        assertThat(gpcAccessStructured.getTaskId()).isEqualTo(TASK_ID);
    }

    private StorageDataWrapper getStorageDataWrapper(String objectName) throws IOException {
        var inputStream = storageConnector.downloadFromStorage(objectName);
        return OBJECT_MAPPER.readValue(new InputStreamReader(inputStream), StorageDataWrapper.class);
    }

    private void assertThatObjectCreated(StorageDataWrapper storageDataWrapper, EhrExtractStatus ehrExtractStatus,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        assertThat(storageDataWrapper.getConversationId()).isEqualTo(ehrExtractStatus.getConversationId());
        assertThat(storageDataWrapper.getTaskId()).isEqualTo(ehrExtractStatus.getGpcAccessStructured().getTaskId());
        assertThat(storageDataWrapper.getType()).isEqualTo(structuredTaskDefinition.getTaskType().getTaskTypeHeaderValue());
        assertThat(storageDataWrapper.getResponse()).contains(STRUCTURED_RECORD_FHIR_PROFILE);
    }

    private GetGpcStructuredTaskDefinition buildInvalidNHSNumberStructuredTask(EhrExtractStatus ehrExtractStatus) {
        return GetGpcStructuredTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .nhsNumber("77777")
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .build();
    }
}
