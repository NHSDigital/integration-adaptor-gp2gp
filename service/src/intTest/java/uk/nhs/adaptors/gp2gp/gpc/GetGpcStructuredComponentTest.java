package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorException;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

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
    private static final String COMPONENT_ELEMENT = "<component typeCode=\"COMP\">";
    private static final String EXPECTED_STRUCTURED_RECORD_JSON_FILENAME =
        CONVERSATION_ID.concat("/").concat(CONVERSATION_ID).concat("_gpc_structured.json");

    @Autowired
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private StorageConnector storageConnector;

    @SpyBean
    private MessageContext messageContext;

    @SpyBean
    private StorageConnectorService storageConnectorService;

    @MockBean
    private DetectTranslationCompleteService detectTranslationCompleteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void When_NewStructuredTask_Expect_DatabaseUpdatedAndAddedToObjectStore() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition);

        var ehrExtractUpdated = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId())
            .orElseThrow();
        assertThatInitialRecordWasUpdated(ehrExtractUpdated, ehrExtractStatus);

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition);

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(ehrExtractUpdated);
        verify(messageContext).resetMessageContext();
        verify(storageConnectorService).uploadFile(
            any(),
            eq(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME)
        );
    }

    @Test
    public void When_StructuredTaskRunTwice_Expect_ObjectToBeOverwrittenInStorage() throws IOException {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1);

        GetGpcStructuredTaskDefinition structuredTaskDefinition2 = buildValidStructuredTask(ehrExtractStatus);
        getGpcStructuredTaskExecutor.execute(structuredTaskDefinition2);

        var ehrExtractUpdated = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId())
            .orElseThrow();

        var storageDataWrapper = getStorageDataWrapper(ehrExtractUpdated);
        assertThatObjectCreated(storageDataWrapper, ehrExtractUpdated, structuredTaskDefinition2);

        assertThat(structuredTaskDefinition1.getTaskId()).isNotEqualTo(ehrExtractUpdated.getGpcAccessStructured().getTaskId());

        verify(detectTranslationCompleteService).beginSendingCompleteExtract(ehrExtractUpdated);
        verify(messageContext, times(2)).resetMessageContext();
        verify(storageConnectorService, times(2)).uploadFile(
            any(),
            eq(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME)
        );
    }

    @Test
    public void When_StructuredTaskPatientNotFoundError_Expect_EhrStatusNotUpdatedAndNotSavedToStorage() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        GetGpcStructuredTaskDefinition structuredTaskDefinition1 = buildInvalidNHSNumberStructuredTask(ehrExtractStatus);
        Exception exception = assertThrows(RuntimeException.class, () -> getGpcStructuredTaskExecutor.execute(structuredTaskDefinition1));

        assertOperationOutcome(exception);

        var ehrExtractUpdated = ehrExtractStatusRepository
            .findByConversationId(ehrExtractStatus.getConversationId())
            .orElseThrow();
        assertThat(ehrExtractUpdated.getGpcAccessStructured()).isNull();

        assertThrows(StorageConnectorException.class,
            () -> storageConnector.downloadFromStorage(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME));

        verify(detectTranslationCompleteService, never()).beginSendingCompleteExtract(any());
    }

    private GetGpcStructuredTaskDefinition buildValidStructuredTask(EhrExtractStatus ehrExtractStatus) {
        return GetGpcStructuredTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .build();
    }

    private void assertThatInitialRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated, EhrExtractStatus ehrExtractStatus) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());
        var gpcAccessStructured = ehrExtractStatusUpdated.getGpcAccessStructured();
        assertThat(gpcAccessStructured.getObjectName()).isEqualTo(EXPECTED_STRUCTURED_RECORD_JSON_FILENAME);
        assertThat(gpcAccessStructured.getAccessedAt()).isNotNull();
        assertThat(gpcAccessStructured.getTaskId()).isNotNull();
    }

    private StorageDataWrapper getStorageDataWrapper(EhrExtractStatus ehrExtractStatus) throws IOException {
        String filename = ehrExtractStatus.getConversationId()
            .concat("/")
            .concat(ehrExtractStatus.getConversationId())
            .concat("_gpc_structured.json");

        try (var inputStream = storageConnector.downloadFromStorage(filename)) {
            String storageDataWrapperString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(storageDataWrapperString, StorageDataWrapper.class);
        }
    }

    @SneakyThrows
    private void assertThatObjectCreated(
        StorageDataWrapper storageDataWrapper, EhrExtractStatus ehrExtractStatus, GetGpcStructuredTaskDefinition structuredTaskDefinition
    ) {
        assertThat(storageDataWrapper.getConversationId()).isEqualTo(ehrExtractStatus.getConversationId());
        assertThat(storageDataWrapper.getTaskId()).isEqualTo(ehrExtractStatus.getGpcAccessStructured().getTaskId());
        assertThat(storageDataWrapper.getType()).isEqualTo(structuredTaskDefinition.getTaskType().getTaskName());

        String mhsMessageJson = storageDataWrapper.getData();
        OutboundMessage outboundMessage = objectMapper.readValue(mhsMessageJson, OutboundMessage.class);
        String hl7Message = outboundMessage.getPayload();
        assertThat(hl7Message).contains(EXPECTED_PAYLOAD_TYPE);
        assertThat(hl7Message).contains(EHR_COMPOSITION_ELEMENT);
        assertThat(hl7Message).contains(COMPONENT_ELEMENT);
        assertThatXmlCanBeParsed(hl7Message);
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
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
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
