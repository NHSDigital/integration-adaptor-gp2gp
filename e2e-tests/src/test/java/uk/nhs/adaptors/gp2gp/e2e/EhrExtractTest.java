package uk.nhs.adaptors.gp2gp.e2e;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EhrExtractTest {

    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    public static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    public static final String NHS_NUMBER = "9690937286";
    public static final String FROM_PARTY_ID = "N82668-820670";
    public static final String TO_PARTY_ID = "B86041-822103";
    public static final String FROM_ASID = "200000000359";
    public static final String TO_ASID = "918999198738";
    public static final String FROM_ODS_CODE = "GPC001";
    public static final String TO_ODS_CODE = "B86041";
    public static final String EHR_REQUEST = "ehrRequest";
    public static final String GPC_ACCESS_STRUCTURED = "gpcAccessStructured";
    public static final String GPC_STRUCTURED_FILENAME_EXTENSION = "_gpc_structured.json";

    @Test
    public void When_extractRequestReceived_Expect_ExtractStatusAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = AwaitHelper.waitFor(() -> Mongo.findEhrExtractStatus(conversationId));

        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus);

        var accessStructured = AwaitHelper.waitFor(
            () -> (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));

        assertThatAccessStructuredWasFetched(conversationId, accessStructured);
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
}
