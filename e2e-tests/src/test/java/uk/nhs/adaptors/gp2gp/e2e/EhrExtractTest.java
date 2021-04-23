package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThatCode;
import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.bson.Document;
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

    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String EHR_EXTRACT_REQUEST_NO_DOCUMENTS_TEST_FILE = "/ehrExtractRequestWithNoDocuments.json";
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String NHS_NUMBER = "9690937286";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";
    private static final String FROM_PARTY_ID = "N82668-820670";
    private static final String TO_PARTY_ID = "B86041-822103";
    private static final String FROM_ASID = "200000000359";
    private static final String TO_ASID = "918999198738";
    private static final String FROM_ODS_CODE = "GPC001";
    private static final String TO_ODS_CODE = "B86041";
    private static final String EHR_REQUEST = "ehrRequest";
    private static final String GPC_ACCESS_STRUCTURED = "gpcAccessStructured";
    private static final String GPC_ACCESS_DOCUMENT = "gpcAccessDocument";
    private static final String EHR_EXTRACT_CORE = "ehrExtractCore";
    private static final String EHR_CONTINUE = "ehrContinue";
    private static final String GPC_STRUCTURED_FILENAME_EXTENSION = "_gpc_structured.json";
    private static final String DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE = "AA";

    @Test
    public void When_ExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        // temporarily ignore the test while GPC data is invalid: NIAD-1300
        assumeThatCode(() -> {
            var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
            assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, NHS_NUMBER);

            var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
            assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

            var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
            assertThatAccessDocumentWasFetched(singleDocument);

            var ehrExtractCore = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_EXTRACT_CORE));
            assertThatExtractCoreMessageWasSent(ehrExtractCore);

            var ehrContinue = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_CONTINUE));
            assertThatExtractContinueMessageWasSent(ehrContinue);
        }).doesNotThrowAnyException();
    }

    @Test
    public void When_ExtractRequestReceivedForPatientWithNoDocs_Expect_DatabaseToBeUpdatedAccordingly() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_NO_DOCUMENTS_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        // temporarily ignore the test while GPC data is invalid: NIAD-1300
        assumeThatCode(() -> {
            var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
            assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, NHS_NUMBER_NO_DOCUMENTS);

            var gpcAccessDocument = waitFor(() -> emptyDocumentTaskIsCreated(conversationId));
            assertThatNotDocumentsWereAdded(gpcAccessDocument);

            var ackToRequester = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get("ackToRequester"));
            assertThatAcknowledgementToRequesterWasSent(ackToRequester);
        }).doesNotThrowAnyException();
    }

    private Document theDocumentTaskUpdatesTheRecord(String conversationId) {
        var gpcAccessDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        return getFirstDocumentIfItHasObjectNameOrElseNull(gpcAccessDocument);
    }

    private Document emptyDocumentTaskIsCreated(String conversationId) {
        return (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
    }

    private Document getFirstDocumentIfItHasObjectNameOrElseNull(Document gpcAccessDocument) {
        var documentList = gpcAccessDocument.get("documents", Collections.emptyList());
        if(!documentList.isEmpty()) {
            Document document = (Document) documentList.get(0);
            if (document.get("objectName") != null) {
                return document;
            }
        }
        return null;
    }

    private void assertThatAcknowledgementToRequesterWasSent(Document ackToRequester) {
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
    }

    private void assertThatExtractContinueMessageWasSent(Document ehrContinue) {
        softly.assertThat(ehrContinue).isNotEmpty().isNotEmpty();
        softly.assertThat(ehrContinue.get("received")).isNotNull();
    }

    private void assertThatInitialRecordWasCreated(String conversationId, Document ehrExtractStatus, String nhsNumber) {
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
        softly.assertThat(ehrRequest.get("fromOdsCode")).isEqualTo(FROM_ODS_CODE);
        softly.assertThat(ehrRequest.get("toOdsCode")).isEqualTo(TO_ODS_CODE);

    }

    private void assertThatAccessStructuredWasFetched(String conversationId, Document accessStructured) {
        softly.assertThat(accessStructured.get("objectName")).isEqualTo(conversationId + GPC_STRUCTURED_FILENAME_EXTENSION);
        softly.assertThat(accessStructured.get("accessedAt")).isNotNull();
        softly.assertThat(accessStructured.get("taskId")).isNotNull();
    }

    private void assertThatAccessDocumentWasFetched(Document document) {
        softly.assertThat(document.get("objectName")).isEqualTo(EhrExtractTest.DOCUMENT_ID + ".json");
        softly.assertThat(document.get("accessedAt")).isNotNull();
        softly.assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatExtractCoreMessageWasSent(Document extractCore) {
        softly.assertThat(extractCore.get("sentAt")).isNotNull();
        softly.assertThat(extractCore.get("taskId")).isNotNull();
    }

    private void assertThatNotDocumentsWereAdded(Document gpcAccessDocument) {
        var documentList = gpcAccessDocument.get("documents", Collections.emptyList());
        assertThat(documentList).isEmpty();
    }
}
