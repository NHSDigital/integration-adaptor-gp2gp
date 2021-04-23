package uk.nhs.adaptors.gp2gp.gpc;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorException;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class GetGpcStructuredComponentTest extends BaseTaskTest {
    private static final String PATIENT_NOT_FOUND = "PATIENT_NOT_FOUND";
    private static final String INVALID_NHS_NUMBER = "INVALID_NHS_NUMBER";
    private static final String TEST_EXCEPTION_MESSAGE = "The following error occurred during GPC request: ";
    private static final String EXPECTED_PAYLOAD_TYPE = "RCMR_IN030000UK06";
    private static final String EXPECTED_NHS_NUMBER = "9876543210";
    private static final String EHR_COMPOSITION_ELEMENT = "<ehrComposition classCode=\"COMPOSITION\" moodCode=\"EVN\">";
    private static final List<String> VALID_ERRORS = Arrays.asList(INVALID_NHS_NUMBER, PATIENT_NOT_FOUND);
    private static final String COMPONENT_ELEMENT = "<component typeCode=\"COMP\" >";

    @Autowired
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;
    @SpyBean
    private MessageContext messageContext;
    @MockBean
    private DetectTranslationCompleteService detectTranslationCompleteService;

    @Test
    public void When_NewStructuredTask_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = buildValidStructuredTask(ehrExtractStatus);
        // temporarily ignore the test while GPC data is invalid: NIAD-1300
        assumeThatCode(() -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition))
            .doesNotThrowAnyException();

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition);

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(ehrExtractUpdated);
        verify(messageContext).resetMessageContext();
    }

    @Test
    public void When_StructuredTaskRunTwice_Expect_ObjectToBeOverwrittenInStorage() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildValidStructuredTask(ehrExtractStatus);
        // temporarily ignore the test while GPC data is invalid: NIAD-1300
        assumeThatCode(() -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1))
            .doesNotThrowAnyException();

        GetGpcStructuredTaskDefinition structuredTaskDefinition2 = buildValidStructuredTask(ehrExtractStatus);
        // temporarily ignore the test while GPC data is invalid: NIAD-1300
        assumeThatCode(() -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition2))
            .doesNotThrowAnyException();

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition2);

        assertThat(structuredTaskDefinition1.getTaskId()).isNotEqualTo(ehrExtractUpdated.getGpcAccessStructured().getTaskId());

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(ehrExtractUpdated);
        verify(messageContext, times(2)).resetMessageContext();
    }

    @Test
    public void When_StructuredTaskPatientNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildInvalidNHSNumberStructuredTask(ehrExtractStatus);
        Exception exception = assertThrows(RuntimeException.class, () -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1));

        assertOperationOutcome(exception);

        var ehrExtractUpdated = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(ehrExtractUpdated.getGpcAccessStructured()).isNull();

        assertThrows(StorageConnectorException.class,
            () -> storageConnector.downloadFromStorage(ehrExtractStatus.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION));

        verify(detectTranslationCompleteService, never()).beginSendingCompleteExtract(any());
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

        String hl7Response = storageDataWrapper.getData();
        assertThat(hl7Response).contains(EXPECTED_PAYLOAD_TYPE);
        assertThat(hl7Response).contains(EHR_COMPOSITION_ELEMENT);
        assertThat(hl7Response).contains(COMPONENT_ELEMENT);
        assertThatXmlCanBeParsed(hl7Response);
    }

    @SneakyThrows
    private void assertThatXmlCanBeParsed(String content) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertThat(doc.getDocumentElement().getNodeName()).isEqualTo(EXPECTED_PAYLOAD_TYPE);
    }

    private GetGpcStructuredTaskDefinition buildInvalidNHSNumberStructuredTask(EhrExtractStatus ehrExtractStatus) {
        return GetGpcStructuredTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .nhsNumber(EXPECTED_NHS_NUMBER)
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .build();
    }

    private void assertOperationOutcome(Exception exception) {
        var operationOutcomeString = exception.getMessage().replace(TEST_EXCEPTION_MESSAGE, StringUtils.EMPTY);
        var operationOutcome = FHIR_PARSE_SERVICE.parseResource(operationOutcomeString, OperationOutcome.class).getIssueFirstRep();
        var coding = operationOutcome.getDetails().getCodingFirstRep();

        assertTrue(VALID_ERRORS.contains(coding.getCode()));
        assertTrue(VALID_ERRORS.contains(coding.getDisplay()));
    }
}
