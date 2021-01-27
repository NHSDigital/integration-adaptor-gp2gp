package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public class EhrExtractTest {
    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String NHS_NUMBER = "9690937286";
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
    private static final String GPC_STRUCTURED_FILENAME_EXTENSION = "_gpc_structured.json";
    private static final String DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    
    @Test
    public void When_ExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
                .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessDocumentWasFetched(singleDocument);

        assertThatExtractCoreMessageWasSent((Document) updatedEhrExtractStatus.get(EHR_EXTRACT_CORE));
    }

    private void assertThatInitialRecordWasCreated(String conversationId, Document ehrExtractStatus) {
        assertThat(ehrExtractStatus).isNotNull();
        assertThat(ehrExtractStatus.get("conversationId")).isEqualTo(conversationId);
        assertThat(ehrExtractStatus.get("created")).isNotNull();
        assertThat(ehrExtractStatus.get("updatedAt")).isNotNull();
        var ehrRequest = (Document) ehrExtractStatus.get(EHR_REQUEST);
        assertThat(ehrRequest.get("requestId")).isEqualTo(REQUEST_ID);
        assertThat(ehrRequest.get("nhsNumber")).isEqualTo(NHS_NUMBER);
        assertThat(ehrRequest.get("fromPartyId")).isEqualTo(FROM_PARTY_ID);
        assertThat(ehrRequest.get("toPartyId")).isEqualTo(TO_PARTY_ID);
        assertThat(ehrRequest.get("fromAsid")).isEqualTo(FROM_ASID);
        assertThat(ehrRequest.get("toAsid")).isEqualTo(TO_ASID);
        assertThat(ehrRequest.get("fromOdsCode")).isEqualTo(FROM_ODS_CODE);
        assertThat(ehrRequest.get("toOdsCode")).isEqualTo(TO_ODS_CODE);
    }

    private void assertThatAccessStructuredWasFetched(String conversationId, Document accessStructured) {
        assertThat(accessStructured.get("objectName")).isEqualTo(conversationId + GPC_STRUCTURED_FILENAME_EXTENSION);
        assertThat(accessStructured.get("accessedAt")).isNotNull();
        assertThat(accessStructured.get("taskId")).isNotNull();
    }

    private Document theDocumentTaskUpdatesTheRecord(String conversationId) {
        var gpcAccessDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        return getFirstDocumentIfItHasObjectNameOrElseNull(gpcAccessDocument);
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

    private void assertThatAccessDocumentWasFetched(Document document) {
        assertThat(document.get("objectName")).isEqualTo(EhrExtractTest.DOCUMENT_ID + ".json");
        assertThat(document.get("accessedAt")).isNotNull();
        assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatExtractCoreMessageWasSent(Document extractCore) {
        assertThat(extractCore.get("sentAt")).isNotNull();
        assertThat(extractCore.get("taskId")).isNotNull();
    }
}
