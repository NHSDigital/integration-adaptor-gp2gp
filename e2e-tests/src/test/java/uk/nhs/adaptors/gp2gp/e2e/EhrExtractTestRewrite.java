package uk.nhs.adaptors.gp2gp.e2e;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.UUID;

import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.buildEhrExtractRequestForNoDocuments;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.fetchDocumentList;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.getEnvVar;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.buildEhrExtractRequest;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.fetchObjectFromEhrExtract;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.fetchFirstObjectFromList;
import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;
import static uk.nhs.adaptors.gp2gp.e2e.utils.EhrExtractStatusPaths.*;

@ExtendWith(SoftAssertionsExtension.class)
public class EhrExtractTestRewrite {
    @InjectSoftAssertions
    private SoftAssertions softly;

    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String FROM_PARTY_ID = "N82668-820670";
    private static final String TO_PARTY_ID = "B86041-822103";
    private static final String FROM_ASID = "200000000359";
    private static final String TO_ASID = "918999198738";
    private static final String TO_ODS_CODE = "B86041";
    private static final String ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE = "AA";

    private static final String NHS_NUMBER = "9690937286";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";

    private static final String FROM_ODS_CODE_1 = "GPC001";
    private static final String FROM_ODS_CODE_2 = "B2617";
    private static final String DOCUMENT_ID_NORMAL = "07a6483f-732b-461e-86b6-edb665c45510";

    private final MhsMockRequestsJournal mhsMockRequestsJournal =
        new MhsMockRequestsJournal(getEnvVar("GP2GP_MHS_MOCK_BASE_URL", "http://localhost:8081"));

    @BeforeEach
    void setUp() {
        mhsMockRequestsJournal.deleteRequestsJournal();
    }

    // standard flows
    @Test
    public void When_EhrExtractReceived_Expect_EhrStatusToBeUpdatedWithSuccess() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithoutDocuments(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        assertThatFirstDocumentWasFetched(conversationId, DOCUMENT_ID_NORMAL);
        assertDocumentsSentToMHS(conversationId);
        assertThatNoErrorInfoIsStored(conversationId);
        assertMessagesWereSentToMhs(6, 0, 2);

        String conversationId2 = UUID.randomUUID().toString();
        String ehrExtractRequest2 = buildEhrExtractRequest(conversationId2, NHS_NUMBER, FROM_ODS_CODE_2);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest2);

        assertHappyPathWithoutDocuments(conversationId2, NHS_NUMBER, FROM_ODS_CODE_2);
        assertThatFirstDocumentWasFetched(conversationId2, DOCUMENT_ID_NORMAL);
        assertDocumentsSentToMHS(conversationId2);
        assertThatNoErrorInfoIsStored(conversationId2);
    }

    // test ehrExtract ASR and no Docs
    @Test
    public void When_EhrExtractReceivedForPatientWithNoDocs_Expect_EhrStatusToBeUpdatedWithSuccess() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequestForNoDocuments(conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithoutDocuments(conversationId, NHS_NUMBER_NO_DOCUMENTS, FROM_ODS_CODE_1);
        assertThatNoDocumentsWereAdded(conversationId);
        assertThatNoErrorInfoIsStored(conversationId);

        assertMessagesWereSentToMhs(2, 1, 0);
    }

    // test ehrExtract error thrown

    // chunking flows
    // test ehrExtract large docs

    // test large ehrExtract docs / no docs

    private void assertHappyPathWithoutDocuments(String conversationId, String nhsNumber, String fromOdsCode) {
        assertInitialRecordCreated(conversationId, nhsNumber, fromOdsCode);
        assertThatAccessStructuredWasFetched(conversationId);
        assertThatExtractCoreMessageWasSent(conversationId);
        assertThatExtractContinueMessageWasSent(conversationId);
        assertThatAcknowledgementPending(conversationId);
        assertThatAcknowledgementSentToRequester(conversationId, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
    }

    private void assertInitialRecordCreated(String conversationId, String nhsNumber, String fromODSCode) {
        var ehrExtractStatus = fetchObjectFromEhrExtract(conversationId, "created");
        var ehrRequest = fetchObjectFromEhrExtract(
            conversationId,
            "requestId",
            EHR_REQUEST);
        assertThat(ehrRequest).isNotNull();
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

    private void assertThatAccessStructuredWasFetched(String conversationId) {
        var accessStructured = fetchObjectFromEhrExtract(
            conversationId,
            "objectName",
            GPC_ACCESS_STRUCTURED);
        assertThat(accessStructured).isNotNull();
        softly.assertThat(accessStructured.get("objectName")).isEqualTo(
            conversationId.concat("/").concat(conversationId).concat("_gpc_structured.json")
        );
        softly.assertThat(accessStructured.get("accessedAt")).isNotNull();
        softly.assertThat(accessStructured.get("taskId")).isNotNull();
    }

    private void assertThatFirstDocumentWasFetched(String conversationId, String documentId) {
        var document = fetchFirstObjectFromList(conversationId, GPC_ACCESS_DOCUMENT, DOCUMENTS);
        assertThat(document).isNotNull();
        softly.assertThat(document.get("objectName")).isEqualTo(conversationId.concat("/").concat(documentId).concat(".json"));
        softly.assertThat(document.get("accessedAt")).isNotNull();
        softly.assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatExtractCoreMessageWasSent(String conversationId) {
        var extractCore = fetchObjectFromEhrExtract(
            conversationId,
            "sentAt",
            EHR_EXTRACT_CORE);
        assertThat(extractCore).isNotNull();
        softly.assertThat(extractCore.get("sentAt")).isNotNull();
        softly.assertThat(extractCore.get("taskId")).isNotNull();
    }

    private void assertThatExtractContinueMessageWasSent(String conversationId) {
        var ehrContinue = fetchObjectFromEhrExtract(
            conversationId,
            "received",
            EHR_CONTINUE);
        assertThat(ehrContinue).isNotNull();
        softly.assertThat(ehrContinue).isNotEmpty();
        softly.assertThat(ehrContinue.get("received")).isNotNull();
    }

    private void assertThatAcknowledgementPending(String conversationId) {
        var ackToRequester = fetchObjectFromEhrExtract(
            conversationId,
            "messageId",
            ACK_PENDING);
        assertThat(ackToRequester).isNotNull();
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
    }

    private void assertThatAcknowledgementSentToRequester(String conversationId, String typeCode) {
        var ackToRequester = fetchObjectFromEhrExtract(
            conversationId,
            "messageId",
            ACK_TO_REQUESTER);
        assertThat(ackToRequester).isNotNull();
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(typeCode);
    }

    private void assertDocumentsSentToMHS(String conversationId) {
        var document = waitFor(() -> fetchFirstObjectFromList(conversationId, GPC_ACCESS_DOCUMENT, DOCUMENTS));
        var sentToMhs = (Document)document.get("sentToMhs");

        softly.assertThat(sentToMhs.get("messageId")).isNotNull();
        softly.assertThat(sentToMhs.get("sentAt")).isNotNull();
        softly.assertThat(sentToMhs.get("taskId")).isNotNull();
    }

    private void assertThatNoDocumentsWereAdded(String conversationId) {
        var documentList = fetchDocumentList(conversationId);
        assertThat(documentList).isEmpty();
    }

    private void assertThatNoErrorInfoIsStored(String conversationId) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");
        assertThat(error).isNull();
    }

    private void assertMessagesWereSentToMhs(
        int expectedNumberOfMessages,
        int expectedAttachments,
        int expectedExternalAttachments
    ) throws InterruptedException, IOException {
        var mhsMockRequests = mhsMockRequestsJournal.getRequestsJournal();
        assertThat(mhsMockRequests).hasSize(expectedNumberOfMessages);

        var ehrExtractMhsRequest = mhsMockRequests.get(0);
        assertThat(ehrExtractMhsRequest.getAttachments()).hasSize(expectedAttachments);
        assertThat(ehrExtractMhsRequest.getExternalAttachments()).hasSize(expectedExternalAttachments);
    }
}
