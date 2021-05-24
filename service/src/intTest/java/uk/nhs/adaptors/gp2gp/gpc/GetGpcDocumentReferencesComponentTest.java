package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GetGpcDocumentReferencesComponentTest extends BaseTaskTest {
    private static final String NHS_NUMBER_WITH_DOCUMENT = "9690937286";
    private static final String NHS_NUMBER_WITHOUT_DOCUMENT = "9690937294";
    private static final String NHS_NUMBER_INVALID = "ASDF";
    private static final String PATIENT_NOT_FOUND = "9876543210";
    private static final String VALID_DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String INVALID_NHS_NUMBER = "INVALID_NHS_NUMBER";

    @Autowired
    private GetGpcDocumentReferencesTaskExecutor gpcFindDocumentsTaskExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private GpcConfiguration configuration;

    @MockBean
    private DetectTranslationCompleteService detectTranslationCompleteService;

    private EhrExtractStatus addTestDataToDatabase() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setGpcAccessDocument(EhrExtractStatus.GpcAccessDocument.builder()
            .documents(prepareAccessDocuments())
            .build());
        return ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    @Test
    public void When_FindDocumentTaskIsStartedForPatientWithDocument_Expect_DatabaseToBeUpdated() {
        var ehrExtractStatus = addTestDataToDatabase();
        assertThatAccessRecordWasOverwritten(ehrExtractStatus);
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, NHS_NUMBER_WITH_DOCUMENT);
        gpcFindDocumentsTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThatAccessRecordWasUpdated(updatedEhrExtractStatus, ehrExtractStatus, taskDefinition);
        verify(taskDispatcher).createTask(buildGetDocumentTask(taskDefinition));
        verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus);
    }

    @Test
    public void When_FindDocumentTaskIsStartedForPatientWithoutDocument_Expect_DatabaseToNotBeUpdated() {
        var ehrExtractStatus = addTestDataToDatabase();
        assertThatAccessRecordWasOverwritten(ehrExtractStatus);
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, NHS_NUMBER_WITHOUT_DOCUMENT);
        gpcFindDocumentsTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThat(updatedEhrExtractStatus.getGpcAccessDocument().getDocuments().size()).isEqualTo(0);
        verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus);
    }

    @Test
    public void When_FindDocumentTaskIsStarted_Expect_PatientNotFound() {
        var ehrExtractStatus = addTestDataToDatabase();
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, PATIENT_NOT_FOUND);
        gpcFindDocumentsTaskExecutor.execute(taskDefinition);

        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId()).get();
        assertThat(updatedEhrExtractStatus.getGpcAccessDocument().getPatientId()).isNull();
        verify(detectTranslationCompleteService).beginSendingCompleteExtract(updatedEhrExtractStatus);
    }

    @Test
    public void When_InvalidNhsNumberIsSupplied_Expect_OperationOutcomeResponse() {
        var ehrExtractStatus = addTestDataToDatabase();
        var taskDefinition = buildFindDocumentTask(ehrExtractStatus, NHS_NUMBER_INVALID);
        var exception = assertThrows(GpConnectException.class, () -> gpcFindDocumentsTaskExecutor.execute(taskDefinition));

        assertOperationOutcome(exception);
        verify(detectTranslationCompleteService, never()).beginSendingCompleteExtract(any(EhrExtractStatus.class));
    }

    private List<EhrExtractStatus.GpcAccessDocument.GpcDocument> prepareAccessDocuments() {
        return List.of(EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
            .documentId(EhrStatusConstants.DOCUMENT_ID)
            .accessDocumentUrl(EhrStatusConstants.GPC_ACCESS_DOCUMENT_URL)
            .build()
        );
    }

    private GetGpcDocumentReferencesTaskDefinition buildFindDocumentTask(EhrExtractStatus ehrExtractStatus, String nhsNumber) {
        return GetGpcDocumentReferencesTaskDefinition.builder()
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .taskId(UUID.randomUUID().toString())
            .nhsNumber(nhsNumber)
            .build();
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        var documentUrl = buildDocumentUrl();
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(VALID_DOCUMENT_ID)
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(documentUrl)
            .build();
    }

    private void assertThatAccessRecordWasUpdated(EhrExtractStatus ehrExtractStatusUpdated,
        EhrExtractStatus ehrExtractStatus,
        GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        assertThat(ehrExtractStatusUpdated.getUpdatedAt()).isNotEqualTo(ehrExtractStatus.getUpdatedAt());

        assertThat(ehrExtractStatusUpdated.getGpcAccessDocument().getDocuments()).hasSize(1);
        var gpcDocument = ehrExtractStatusUpdated.getGpcAccessDocument().getDocuments().get(0);
        var documentUrl = buildDocumentUrl();

        assertThat(gpcDocument).isNotNull();
        assertThat(gpcDocument.getAccessDocumentUrl()).isEqualTo(documentUrl);
        assertThat(gpcDocument.getAccessedAt()).isNotNull();
        assertThat(gpcDocument.getTaskId()).isEqualTo(taskDefinition.getTaskId());
    }

    private void assertThatAccessRecordWasOverwritten(EhrExtractStatus ehrExtractStatus) {
        var updatedEhrExtractStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(updatedEhrExtractStatus.getCreated()).isEqualTo(ehrExtractStatus.getCreated());
        assertThat(updatedEhrExtractStatus.getUpdatedAt()).isEqualTo(ehrExtractStatus.getUpdatedAt());

        EhrExtractStatus.EhrRequest updatedEhrRequest = updatedEhrExtractStatus.getEhrRequest();
        EhrExtractStatus.EhrRequest ehrRequest = ehrExtractStatus.getEhrRequest();
        assertThat(updatedEhrRequest.getRequestId()).isEqualTo(ehrRequest.getRequestId());
        assertThat(updatedEhrRequest.getNhsNumber()).isEqualTo(ehrRequest.getNhsNumber());

    }

    private void assertOperationOutcome(Exception exception) {
        var operationOutcomeString = exception.getMessage().replace("The following error occurred during GPC request: ", "");
        var operationOutcome = FHIR_PARSE_SERVICE.parseResource(operationOutcomeString, OperationOutcome.class).getIssueFirstRep();
        var coding = operationOutcome.getDetails().getCodingFirstRep();
        assertThat(coding.getCode()).isEqualTo(INVALID_NHS_NUMBER);
        assertThat(coding.getDisplay()).isEqualTo(INVALID_NHS_NUMBER);
    }

    private String buildDocumentUrl() {
        return configuration.getUrl() + configuration.getDocumentEndpoint() + VALID_DOCUMENT_ID;
    }
}
