package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class GetGpcStructuredComponentTest extends BaseTaskTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EXPECTED_ERROR_RESPONSE = "The following error occurred during Gpc Request: "
        + "{\"resourceType\":\"OperationOutcome\",\"meta\":{\"profile\":[\"https://fhir.nhs"
        + ".uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1\"]},\"issue\":[{\"severity\":\"error\",\"code\":\"value\","
        + "\"details\":{\"coding\":[{\"system\":\"https://fhir.nhs.uk/STU3/CodeSystem/Spine-ErrorOrWarningCode-1\",\"code\":"
        + "\"INVALID_NHS_NUMBER\",\"display\":\"INVALID_NHS_NUMBER\"}]},\"diagnostics\":\"Invalid NHS number submitted: 77777\"}]}";
    @Autowired
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;
    @Autowired
    private GpcTaskAggregateService gpcTaskAggregateService;

    @Test
    public void When_NewStructuredTask_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition);
    }

    @Test
    public void When_StructuredTaskRunTwice_Expect_ObjectToBeOverwrittenInStroage() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1);

        GetGpcStructuredTaskDefinition structuredTaskDefinition2 = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition2);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition2);

        assertThat(structuredTaskDefinition1.getTaskId()).isNotEqualTo(ehrExtractUpdated.getGpcAccessStructured().getTaskId());
    }

    @Test
    public void When_StructuredTaskPatientNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildInvalidNHSNumberStructuredTask(ehrExtractStatus);
        Exception exception = assertThrows(RuntimeException.class, () -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1));
        assertThat(exception.getMessage()).isEqualTo(EXPECTED_ERROR_RESPONSE);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtractUpdated.getGpcAccessStructured()).isNull();

        assertThrows(StorageConnectorException.class,
            () -> storageConnector.downloadFromStorage(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION));
    }

    private GetGpcStructuredTaskDefinition buildValidStructuredTask(EhrExtractStatus ehrExtractStatus) {
        return GetGpcStructuredTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .build();
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var gpcAccessStructured = ehrExtractStatusUpdated.getGpcAccessStructured();
        assertThat(gpcAccessStructured.getObjectName()).isEqualTo(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        assertThat(gpcAccessStructured.getAccessedAt()).isNotNull();
        assertThat(gpcAccessStructured.getTaskId()).isNotNull();
    }

    private StorageDataWrapper getStorageDataWrapper(EhrExtractStatus ehrExtractStatus) throws IOException {
        var inputStream = storageConnector.downloadFromStorage(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        String storageDataWrapperString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readValue(storageDataWrapperString, StorageDataWrapper.class);
    }

    private void assertThatObjectCreated(StorageDataWrapper storageDataWrapper, EhrExtractStatus ehrExtractStatus,
        GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        assertThat(storageDataWrapper.getConversationId()).isEqualTo(ehrExtractStatus.getConversationId());
        assertThat(storageDataWrapper.getTaskId()).isEqualTo(ehrExtractStatus.getGpcAccessStructured().getTaskId());
        assertThat(storageDataWrapper.getType()).isEqualTo(structuredTaskDefinition.getTaskType().getTaskTypeHeaderValue());
        assertThat(storageDataWrapper.getResponse()).contains("https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-StructuredRecord"
            + "-Bundle-1");
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
