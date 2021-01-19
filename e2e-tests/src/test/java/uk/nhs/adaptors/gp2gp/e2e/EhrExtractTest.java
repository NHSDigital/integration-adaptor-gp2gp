package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
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
    private static final String GPC_ACCESS_DOCUMENTS = "gpcAccessDocuments";
    private static final String GPC_STRUCTURED_FILENAME_EXTENSION = "_gpc_structured.json";
    private static final String TASK_ID = "test-task-id";
    private static final String UPDATED_TASK_ID = "updated-task-id";
    private static final String DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String GPC_ACCESS_DOCUMENT_BODY_TEMPLATE = "{\"taskId\":\"%s\"," +
        "\"requestId\":\"test-request-id\"," +
        "\"conversationId\":\"%s\"," +
        "\"documentId\":\"%s\"," +
        "\"fromAsid\":\"200000000359\"," +
        "\"toAsid\":\"918999198738\"}";

    @Test
    public void When_ExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
                .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = AwaitHelper.waitFor(() -> Mongo.findEhrExtractStatus(conversationId));

        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus);

        Instant accessedAt = Instant.now();
        Mongo.addAccessDocument(conversationId, DOCUMENT_ID, TASK_ID, accessedAt);
        MessageQueue.sendToMhsTaskQueue(prepareTaskQueueMessage(conversationId));

        assertThatAccessStructuredWasFetched(conversationId, (Document) ehrExtractStatus.get(GPC_ACCESS_STRUCTURED));

        var extractStatusWithUpdatedFields = AwaitHelper.waitFor(
            () -> Mongo.findEhrExtractStatusWithUpdatedGpcAccessDocument(conversationId, UPDATED_TASK_ID));
        assertThatAccessDocumentWasFetched(DOCUMENT_ID, accessedAt, (List) extractStatusWithUpdatedFields.get(GPC_ACCESS_DOCUMENTS));
    }

    private static String prepareTaskQueueMessage(String conversationId) {
        return String.format(GPC_ACCESS_DOCUMENT_BODY_TEMPLATE, UPDATED_TASK_ID, conversationId, DOCUMENT_ID);
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

    private void assertThatAccessDocumentWasFetched(String documentId, Instant accessedAt, List<Document> accessDocuments) {
        Document document = accessDocuments.get(0);
        assertThat(document.get("objectName")).isEqualTo(documentId + ".json");
        assertThat(document.get("accessedAt")).isNotEqualTo(accessedAt);
        assertThat(document.get("taskId")).isEqualTo(UPDATED_TASK_ID);
    }
}
