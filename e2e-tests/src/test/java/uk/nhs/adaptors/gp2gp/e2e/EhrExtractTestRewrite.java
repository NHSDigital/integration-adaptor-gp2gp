package uk.nhs.adaptors.gp2gp.e2e;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import uk.nhs.adaptors.gp2gp.MessageQueue;

import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.getEnvVar;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.buildEhrExtractRequest;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.fetchObjectFromEhrExtract;
import static uk.nhs.adaptors.gp2gp.e2e.utils.e2eTestsHelper.fetchFirstObjectFromList;
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
    private static final String FROM_ODS_CODE_1 = "GPC001";
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

        assertInitialRecordCreated(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        assertThatAccessStructuredWasFetched(conversationId);
        assertThatFirstDocumentWasFetched(conversationId, DOCUMENT_ID_NORMAL);
        assertThatExtractCoreMessageWasSent(conversationId);
        assertThatExtractContinueMessageWasSent(conversationId);
        assertThatAcknowledgementPending(conversationId, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
    }

    // test ehrExtract ASR and no Docs

    // test ehrExtract error thrown

    // chunking flows
    // test ehrExtract large docs

    // test large ehrExtract docs / no docs

    // assertions
    private void assertInitialRecordCreated(String conversationId, String nhsNumber, String fromODSCode) {
        var ehrExtractStatus = fetchObjectFromEhrExtract(conversationId);
        var ehrRequest = fetchObjectFromEhrExtract(conversationId, EHR_REQUEST);

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
        var accessStructured = fetchObjectFromEhrExtract(conversationId, GPC_ACCESS_STRUCTURED);
        softly.assertThat(accessStructured.get("objectName")).isEqualTo(
            conversationId.concat("/").concat(conversationId).concat("_gpc_structured.json")
        );
        softly.assertThat(accessStructured.get("accessedAt")).isNotNull();
        softly.assertThat(accessStructured.get("taskId")).isNotNull();
    }

    private void assertThatFirstDocumentWasFetched(String conversationId, String documentId) {
        var document = fetchFirstObjectFromList(conversationId, GPC_ACCESS_DOCUMENT, DOCUMENTS);
        softly.assertThat(document.get("objectName")).isEqualTo(conversationId.concat("/").concat(documentId).concat(".json"));
        softly.assertThat(document.get("accessedAt")).isNotNull();
        softly.assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatExtractCoreMessageWasSent(String conversationId) {
        var extractCore = fetchObjectFromEhrExtract(conversationId, EHR_EXTRACT_CORE);
        softly.assertThat(extractCore.get("sentAt")).isNotNull();
        softly.assertThat(extractCore.get("taskId")).isNotNull();
    }

    private void assertThatExtractContinueMessageWasSent(String conversationId) {
        var ehrContinue = fetchObjectFromEhrExtract(conversationId, EHR_CONTINUE);
        softly.assertThat(ehrContinue).isNotEmpty().isNotEmpty();
        softly.assertThat(ehrContinue.get("received")).isNotNull();
    }

    private void assertThatAcknowledgementPending(String conversationId, String typeCode) {
        var ackToRequester = fetchObjectFromEhrExtract(conversationId, ACK_TO_REQUESTER);
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(typeCode);
    }
}
