package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.bson.Document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.util.StringUtils;
import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class EhrExtractTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    private static final String NHS_NUMBER = "9690937286";
    private static final String NOT_EXISTING_PATIENT_NHS_NUMBER = "9876543210";
    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String EHR_EXTRACT_REQUEST_NO_DOCUMENTS_TEST_FILE = "/ehrExtractRequestWithNoDocuments.json";
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_1 = "9690937819";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_2 = "9690937841";
    private static final String FROM_PARTY_ID = "N82668-820670";
    private static final String TO_PARTY_ID = "B86041-822103";
    private static final String FROM_ASID = "200000000359";
    private static final String TO_ASID = "918999198738";
    private static final String FROM_ODS_CODE_1 = "GPC001";
    private static final String FROM_ODS_CODE_2 = "B2617";
    private static final String TO_ODS_CODE = "B86041";
    private static final String EHR_REQUEST = "ehrRequest";
    private static final String GPC_ACCESS_STRUCTURED = "gpcAccessStructured";
    private static final String GPC_ACCESS_DOCUMENT = "gpcAccessDocument";
    private static final String SENT_TO_MHS = "sentToMhs";
    private static final String MESSAGE_ID = "messageId";
    private static final String EHR_EXTRACT_CORE = "ehrExtractCore";
    private static final String EHR_CONTINUE = "ehrContinue";
    private static final String DOCUMENT_ID_NORMAL = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String DOCUMENT_ID_LARGE = "11737b22-8cff-47e2-b741-e7f27c8c61a8";
    private static final String DOCUMENT_ID_LARGE_2 = "29c434d6-ad47-415f-b5f5-fd1dc2941d8d";
    private static final String ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE = "AA";
    private static final String NEGATIVE_ACKNOWLEDGEMENT_TYPE_CODE = "AE";
    private static final String CONVERSATION_ID_PLACEHOLDER = "%%ConversationId%%";
    private static final String FROM_ODS_CODE_PLACEHOLDER = "%%From_ODS_Code%%";
    private static final String NHS_NUMBER_PLACEHOLDER = "%%NHSNumber%%";
    private static final String GET_GPC_STRUCTURED_TASK_NAME = "GET_GPC_STRUCTURED";
    private static final String ACK_TO_REQUESTER = "ackToRequester";
    private static final String ACK_TO_PENDING = "ackPending";

    private final MhsMockRequestsJournal mhsMockRequestsJournal =
        new MhsMockRequestsJournal(getEnvVar("GP2GP_MHS_MOCK_BASE_URL", "http://localhost:8081"));

    @BeforeEach
    void setUp() {
        mhsMockRequestsJournal.deleteRequestsJournal();
    }

    @Test
    public void When_ExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithDocs(conversationId, FROM_ODS_CODE_1, NHS_NUMBER, DOCUMENT_ID_NORMAL);

        String conversationId2 = UUID.randomUUID().toString();
        String ehrExtractRequest2 = buildEhrExtractRequest(conversationId2, NHS_NUMBER, FROM_ODS_CODE_2);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest2);

        assertHappyPathWithDocs(conversationId2, FROM_ODS_CODE_2, NHS_NUMBER, DOCUMENT_ID_NORMAL);
    }

    @Test
    public void When_ExtractRequestReceivedForPatientWithNoDocs_Expect_DatabaseToBeUpdatedAccordingly() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_NO_DOCUMENTS_TEST_FILE), StandardCharsets.UTF_8)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, NHS_NUMBER_NO_DOCUMENTS, FROM_ODS_CODE_1);

        var gpcAccessDocument = waitFor(() -> emptyDocumentTaskIsCreated(conversationId));
        assertThat(gpcAccessDocument).isEmpty();

        var ackToPending = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_PENDING));
        assertThatAcknowledgementPending(ackToPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
        assertThatNoErrorInfoIsStored(conversationId);
    }

    @Test
    public void When_ExtractRequestReceivedForNotExistingPatient_Expect_ErrorUpdatedInDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NOT_EXISTING_PATIENT_NHS_NUMBER, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, NOT_EXISTING_PATIENT_NHS_NUMBER, FROM_ODS_CODE_1);

        var ackToRequester = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_REQUESTER));
        assertThatNegativeAcknowledgementToRequesterWasSent(ackToRequester, NEGATIVE_ACKNOWLEDGEMENT_TYPE_CODE);
        assertThatErrorInfoIsStored(conversationId, GET_GPC_STRUCTURED_TASK_NAME);
    }

    @Test
    public void When_ExtractRequestReceivedWithDocumentSizeEqualThreshold_Expect_LargeDocumentIsSentAsOne() throws Exception {
        // file size: 31216
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_1, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertMultipleDocsSent(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_1, DOCUMENT_ID_LARGE, 1);
    }

    @Test
    public void When_ExtractRequestReceivedWithDocumentSizeLargerThanThreshold_Expect_LargeDocumentIsSplit() throws Exception {
        // file size: 62428
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertMultipleDocsSent(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, DOCUMENT_ID_LARGE_2, 3);
    }

    private void assertThatEhrExtractWasAttachedAsDocument(Document gpcAccessStructured) {
        assertThat(gpcAccessStructured.get("attachment")).isNotNull();
    }

    private void assertMultipleDocsSent(String conversationId, String nhsNumber, String documentId, int arraySize) {
        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, FROM_ODS_CODE_1);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessDocumentWasFetched(conversationId, singleDocument, documentId);

        var messageIds = waitFor(() -> getTheSplitDocumentIds(conversationId));
        softly.assertThat(messageIds.size()).isEqualTo(arraySize);
    }

    private void assertHappyPathWithDocs(String conversationId, String fromODSCode, String nhsNumber, String documentId) {
        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, fromODSCode);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessDocumentWasFetched(conversationId, singleDocument, documentId);

        var ehrExtractCore = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_EXTRACT_CORE));
        assertThatExtractCoreMessageWasSent(ehrExtractCore);

        var ehrContinue = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_CONTINUE));
        assertThatExtractContinueMessageWasSent(ehrContinue);

//        var sentToMhs = (Document) waitFor(() -> getSentToMhs(conversationId));
//        assertThat(sentToMhs).isNotNull();

        var ackPending = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_PENDING));
        assertThatAcknowledgementPending(ackPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
    }

    private String buildEhrExtractRequest(String conversationId, String notExistingPatientNhsNumber, String fromODSCode) throws IOException {
        return IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), StandardCharsets.UTF_8)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId)
            .replace(NHS_NUMBER_PLACEHOLDER, notExistingPatientNhsNumber)
            .replace(FROM_ODS_CODE_PLACEHOLDER, fromODSCode);
    }

    private Document theDocumentTaskUpdatesTheRecord(String conversationId) {
        var gpcAccessDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        return getFirstDocumentIfItHasObjectNameOrElseNull(gpcAccessDocument);
    }

    private List<Object> emptyDocumentTaskIsCreated(String conversationId) {
        var gpcAccessDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        if (gpcAccessDocument == null) {
            return null;
        }
        var documentList = waitFor(() -> gpcAccessDocument.get("documents", Collections.emptyList()));
        return documentList;
    }

    private Document getFirstDocumentIfItHasObjectNameOrElseNull(Document gpcAccessDocument) {
        var documentList = gpcAccessDocument.get("documents", Collections.emptyList());
        if (!documentList.isEmpty()) {
            Document document = (Document) documentList.get(0);
            if (document.get("objectName") != null) {
                return document;
            }
        }
        return null;
    }

    private List<Object> getTheSplitDocumentIds(String conversationId) {
        var document = (Document) theDocumentTaskUpdatesTheRecord(conversationId);
        var sentToMhs = (Document) document.get(SENT_TO_MHS);

        if (sentToMhs != null) {
            return sentToMhs.get(MESSAGE_ID, Collections.emptyList());
        }
        return null;
    }

    private void assertThatAcknowledgementPending(Document ackToRequester, String typeCode) {
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(typeCode);
    }

    private void assertThatNegativeAcknowledgementToRequesterWasSent(Document ackToRequester, String typeCode) {
        // TODO: error code and message to be prepared as part of NIAD-1524
        assertThatAcknowledgementPending(ackToRequester, typeCode);
        softly.assertThat(ackToRequester.get("reasonCode")).isEqualTo("18");
        softly.assertThat(ackToRequester.get("detail")).isEqualTo("An error occurred when executing a task");
    }

    private void assertThatNoErrorInfoIsStored(String conversationId) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");
        assertThat(error).isNull();
    }

    private void assertThatErrorInfoIsStored(String conversationId, String expectedTaskType) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");

        softly.assertThat(error.get("occurredAt")).isNotNull();
        softly.assertThat(error.get("code")).isEqualTo("18");
        softly.assertThat(error.get("message")).isEqualTo("An error occurred when executing a task");
        softly.assertThat(error.get("taskType")).isEqualTo(expectedTaskType);
    }

    private void assertThatExtractContinueMessageWasSent(Document ehrContinue) {
        softly.assertThat(ehrContinue).isNotEmpty().isNotEmpty();
        softly.assertThat(ehrContinue.get("received")).isNotNull();
    }

    private Document getSentToMhs(String conversationId) {
        var ehrDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        var document = getFirstDocumentIfItHasObjectNameOrElseNull(ehrDocument);
        if (document != null) {
            var ehrCommon = (Document) document.get("sentToMhs");
            if (ehrCommon != null
                && ehrCommon.get("messageId") != null
                && ehrCommon.get("sentAt") != null
                && ehrCommon.get("taskId") != null) {
                return ehrCommon;
            }
        }
        return null;
    }

    private void assertThatInitialRecordWasCreated(String conversationId, Document ehrExtractStatus, String nhsNumber, String fromODSCode) {
        var ehrRequest = (Document) ehrExtractStatus.get(EHR_REQUEST);
        softly.assertThat(ehrExtractStatus).isNotNull();
        softly.assertThat(ehrExtractStatus.get("conversationId")).isEqualTo(conversationId);
        softly.assertThat(ehrExtractStatus.get("created")).isNotNull();
        softly.assertThat(ehrExtractStatus.get("updatedAt")).isNotNull();
        softly.assertThat(ehrRequest.get("requestId")).isEqualTo(REQUEST_ID);
        softly.assertThat(ehrRequest.get("nhsNumber")).isEqualTo(nhsNumber);
        softly.assertThat(ehrRequest.get("fromPartyId")).isEqualTo(FROM_PARTY_ID);
        softly.assertThat(ehrRequest.get("toPartyId")).isEqualTo(TO_PARTY_ID);
        softly.assertThat(ehrRequest.get("fromAsid")).isEqualTo(FROM_ASID);
        softly.assertThat(ehrRequest.get("toAsid")).isEqualTo(TO_ASID);
        softly.assertThat(ehrRequest.get("fromOdsCode")).isEqualTo(fromODSCode);
        softly.assertThat(ehrRequest.get("toOdsCode")).isEqualTo(TO_ODS_CODE);
    }

    private void assertThatAccessStructuredWasFetched(String conversationId, Document accessStructured) {
        softly.assertThat(accessStructured.get("objectName")).isEqualTo(
            conversationId.concat("/").concat(conversationId).concat("_gpc_structured.json")
        );
        softly.assertThat(accessStructured.get("accessedAt")).isNotNull();
        softly.assertThat(accessStructured.get("taskId")).isNotNull();
    }

    private void assertThatAccessDocumentWasFetched(String conversationId, Document document, String documentId) {
        softly.assertThat(document.get("objectName")).isEqualTo(conversationId.concat("/").concat(documentId).concat(".json"));
        softly.assertThat(document.get("accessedAt")).isNotNull();
        softly.assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatExtractCoreMessageWasSent(Document extractCore) {
        softly.assertThat(extractCore.get("sentAt")).isNotNull();
        softly.assertThat(extractCore.get("taskId")).isNotNull();
    }

    private static String getEnvVar(String name, String defaultValue) {
        var value = System.getenv(name);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }
}
