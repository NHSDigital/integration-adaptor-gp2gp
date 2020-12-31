package uk.nhs.adaptors.gp2gp;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EhrExtractTest {

    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    public static final String EXTRACT_ID = "test-extract-id";
    public static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    public static final String NHS_NUMBER = "9692294935";
    public static final String FROM_PARTY_ID = "N82668-820670";
    public static final String TO_PARTY_ID = "B86041-822103";
    public static final String FROM_ASID = "200000000205";
    public static final String TO_ASID = "200000001161";
    public static final String FROM_ODS_CODE = "N82668";
    public static final String TO_ODS_CODE = "B86041";

    @Test
    public void When_extractRequestReceived_Expect_ExtractStatusAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = IOUtils.toString(getClass()
                .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), Charset.defaultCharset());
        ehrExtractRequest = ehrExtractRequest.replace("%%ConversationId%%", conversationId);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = AwaitHelper.waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThat(ehrExtractStatus).isNotNull();
        assertThat(ehrExtractStatus.get("conversationId")).isEqualTo(conversationId);
    }

}
