package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;
import static uk.nhs.adaptors.gp2gp.e2e.model.EhrStatus.AttachmentStatus.FileStatus.PLACEHOLDER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.xmlunit.assertj.XmlAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;
import uk.nhs.adaptors.gp2gp.e2e.model.EhrStatus;

@ExtendWith(SoftAssertionsExtension.class)
public class EhrExtractTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    //AA-DR = AbsentAttachment DocumentReference
    //NORMAL-DR = Non-AbsentAttachment DocumentReference
    private static final String NHS_NUMBER_WITH_AA_DR = "9690937286";
    private static final String NHS_NUMBER_WITH_NORMAL_DR = "9690937287";
    private static final String NHS_NUMBER_NOT_EXISTING_PATIENT = "9600000001";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_1 = "9690937819";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_2 = "9690937841";
    private static final String NHS_NUMBER_TWO_DOCUMENTS = "9690937789";
    private static final String NHS_NUMBER_THREE_SMALL_AA_DOCUMENTS = "9690937419";
    private static final String NHS_NUMBER_THREE_SMALL_NORMAL_DOCUMENTS = "9690937420";
    private static final String NHS_NUMBER_LARGE_PAYLOAD = "9690937421";
    private static final String NHS_NUMBER_LARGE_ATTACHMENT_DOCX = "9388098434";
    private static final String NHS_NUMBER_NO_RELATIONSHIP = "9600000010";
    private static final String NHS_NUMBER_PATIENT_NOT_FOUND = "9600000009";
    private static final String NHS_NUMBER_INVALID_DEMOGRAPHIC = "9600000002";
    private static final String NHS_NUMBER_INVALID_RESOURCE = "9600000003";
    private static final String NHS_NUMBER_INVALID_PARAMETER = "9600000004";
    private static final String NHS_NUMBER_BAD_REQUEST = "9600000005";
    private static final String NHS_NUMBER_INTERNAL_SERVER_ERROR = "9600000006";
    private static final String NHS_NUMBER_INVALID_NHS_NUMBER = "123456789";
    private static final String NHS_NUMBER_RESPONSE_HAS_MALFORMED_DATE = "9690872294";
    private static final String NHS_NUMBER_RESPONSE_MISSING_PATIENT_RESOURCE = "2906543841";
    private static final String NHS_NUMBER_MEDICUS_BASED_ON = "9302014592";
    private static final String NHS_NUMBER_INVALID_CONTENT_TYPE_DOC = "9817280691";
    private static final String NHS_NUMBER_BODY_SITE = "1239577290";
    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String EHR_EXTRACT_REQUEST_WITHOUT_NHS_NUMBER_TEST_FILE = "/ehrExtractRequestWithoutNhsNumber.json";
    private static final String EHR_EXTRACT_REQUEST_NO_DOCUMENTS = "/ehrExtractRequestWithNoDocuments.json";
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
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
    private static final String ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE = "AA";
    private static final String NEGATIVE_ACKNOWLEDGEMENT_TYPE_CODE = "AE";
    private static final String CONVERSATION_ID_PLACEHOLDER = "%%ConversationId%%";
    private static final String FROM_ODS_CODE_PLACEHOLDER = "%%From_ODS_Code%%";
    private static final String NHS_NUMBER_PLACEHOLDER = "%%NHSNumber%%";
    private static final String GET_GPC_STRUCTURED_TASK_NAME = "GET_GPC_STRUCTURED";
    private static final String ACK_TO_REQUESTER = "ackToRequester";
    private static final String ACK_TO_PENDING = "ackPending";
    private static final String NACK_CODE_FAILED_TO_GENERATE_EHR = "10";
    private final static String NACK_CODE_REQUEST_NOT_WELL_FORMED = "18";
    private final static String NACK_CODE_PATIENT_NOT_FOUND = "06";
    private final static String NACK_CODE_INVALID = "19";
    private final static String NACK_CODE_GP_CONNECT_ERROR = "20";
    private final static String NACK_CODE_NO_RELATIONSHIP = "19";
    private final static String NACK_MESSAGE_REQUEST_NOT_WELL_FORMED = "An error occurred processing the initial EHR request";
    private final static String NACK_MESSAGE_NOT_FOUND = "Patient not at surgery.";

    private static final CharSequence XML_NAMESPACE = "/urn:hl7-org:v3:";
    private static final String DOCUMENT_REFERENCE_XPATH_TEMPLATE = "/RCMR_IN030000UK06/ControlActEvent/subject/EhrExtract/component/ehrFolder/component/ehrComposition/component/NarrativeStatement/reference/referredToExternalDocument/text/reference[@value='cid:%s']";
    private static final String BODY_SITE_REFERENCE_XPATH_TEMPLATE = "/RCMR_IN030000UK06/ControlActEvent/subject/EhrExtract/component/ehrFolder/component/ehrComposition/component/ObservationStatement/pertinentInformation/pertinentAnnotation/text[contains(text(), 'BodySite: Rib cage')]";

    private final MhsMockRequestsJournal mhsMockRequestsJournal =
        new MhsMockRequestsJournal(getEnvVar("GP2GP_MHS_MOCK_BASE_URL", "http://localhost:8081"));

    private final String gp2gpBaseUrl = getEnvVar("GP2GP_BASE_URL", "http://localhost:8080");

    private static final Map<String, String> emisPatientsNhsNumbers = Map.of(
        "PWTP2", "9726908671",
        "PWTP3", "9726908698",
        "PWTP4", "9726908701",
        "PWTP5", "9726908728",
        "PWTP6", "9726908736",
        "PWTP7", "9726908744",
        "PWTP9", "9726908752",
        "PWTP10", "9726908760",
        "PWTP11", "9726908779");

    private static final Map<String, String> tppPatientsNhsNumbers = Map.of(
        "PWTP2", "9726908787",
        "PWTP3", "9726908795",
        "PWTP4", "9726908809",
        "PWTP5", "9726908817",
        "PWTP6", "9726908825",
        "PWTP7", "9726908833",
        "PWTP9", "9726908841",
        "PWTP10", "9726908868",
        "PWTP11", "9726908876");

    @BeforeEach
    void setUp() {
        mhsMockRequestsJournal.deleteRequestsJournal();
    }

    @Test
    public void When_ExtractRequestReceivedForPatientWithLargeEhrExtractAnd2AbsentAttachments_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_TWO_DOCUMENTS, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithAbsentAttachments(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_TWO_DOCUMENTS, 2);
        assertMultipleDocumentsRetrieved(conversationId, 3);
    }

    private Document getFinishedEhrExtractStatus(String conversationId) {
        var ehrExtractStatus = Mongo.findEhrExtractStatus(conversationId);
        if (ehrExtractStatus == null) {
            return null;
        }
        var ehrReceivedAcknowledgement = ehrExtractStatus.get("ehrReceivedAcknowledgement", Document.class);
        if (ehrReceivedAcknowledgement != null) {
            var conversationClosed = ehrReceivedAcknowledgement.get("conversationClosed");
            if (conversationClosed != null) {
                return ehrExtractStatus;
            }
        }
        return null;
    }

    private void assertEhrStatusHasPlaceholders(EhrStatus ehrStatus) {
        List<EhrStatus.AttachmentStatus> placeholders = ehrStatus.getAttachmentStatus().stream()
            .filter(status -> status.getFileStatus().equals(PLACEHOLDER))
            .collect(Collectors.toList());

        boolean hasAbsentAttachmentInFilename = placeholders.stream()
            .allMatch(status -> status.getFileName().startsWith("AbsentAttachment"));

        boolean hasPlainTextSuffix = placeholders.stream()
            .allMatch(status -> status.getFileName().endsWith(".txt"));

        assertThat(placeholders.isEmpty())
            .as("Migration should have placeholders")
            .isFalse();

        // TODO: NIAD-2394 - These assertions can be used to ensure AbsentAttachment is appended to a placeholders filename and there is a plain text suffix

//        assertThat(hasAbsentAttachmentInFilename)
//            .as("A placeholder's filename should be prepended with AbsentAttachment")
//            .isTrue();
//
//        assertThat(hasPlainTextSuffix)
//            .as("A placeholder should have a plain text suffix")
//            .isTrue();
    }

    private EhrStatus getEhrStatusForConversation(String conversationId) throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(gp2gpBaseUrl.concat("/ehr-status/").concat(conversationId));

            String response = httpClient.execute(httpGet, httpResponse -> {
                int statusCode = httpResponse.getCode();
                assertThat(statusCode).isEqualTo(200);

                HttpEntity entity = httpResponse.getEntity();

                try {
                    return entity != null ? EntityUtils.toString(entity) : null;
                } catch (ParseException e) {
                    throw new ClientProtocolException(e);
                }
            });

            ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

            return objectMapper.readValue(response, EhrStatus.class);
        }
    }

    private void assertMultipleDocumentsRetrieved(String conversationId, int documentCount) {
        var documentList = waitFor(() -> {
            var extractStatus = ((Document) Mongo.findEhrExtractStatus(conversationId)
                .get(GPC_ACCESS_DOCUMENT));
            if (extractStatus == null) {
                return null;
            }
            return extractStatus.get("documents", Collections.emptyList());
        });

        assertThat(documentList).hasSize(documentCount);
    }

    private void assertMultipleDocsSent(String conversationId, String nhsNumber, int arraySize) {
        var ehrExtractStatus = waitFor(() -> getFinishedEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, FROM_ODS_CODE_1);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        theDocumentTaskUpdatesTheRecord(ehrExtractStatus)
            .forEach(this::assertThatAccessDocumentWasFetched);

        var messageIds = getTheSplitDocumentIds(ehrExtractStatus).get(0);
        softly.assertThat(messageIds).hasSize(arraySize);
    }

    private void assertHappyPathWithAbsentAttachments(String conversationId, String fromODSCode, String nhsNumber, int absentAttachmentCount) {
        var ehrExtractStatus = waitFor(() -> getFinishedEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, fromODSCode);

        var gpcAccessStructured = ehrExtractStatus.get(GPC_ACCESS_STRUCTURED, Document.class);
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);
        
        var documents = (List<Document>) waitFor(() -> theDocumentTaskUpdatesTheRecord(ehrExtractStatus));
        assertThat(documents).hasSize(absentAttachmentCount + 1); //because large ehr extract is here as well
        for (int i = 1; i < absentAttachmentCount; i++) {
            assertThatAccessAbsentDocumentWasFetched(documents.get(i));
        }

        var ehrExtractCore = ehrExtractStatus.get(EHR_EXTRACT_CORE, Document.class);
        assertThatExtractCoreMessageWasSent(ehrExtractCore);

        var ehrContinue = ehrExtractStatus.get(EHR_CONTINUE, Document.class);
        assertThatExtractContinueMessageWasSent(ehrContinue);

        var ackPending = ehrExtractStatus.get(ACK_TO_PENDING, Document.class);
        assertThatAcknowledgementPending(ackPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);

        var sentToMhs = fetchSentToMhsForDocuments(ehrExtractStatus);
        assertThat(sentToMhs.get("messageId")).isNotNull();
        assertThat(sentToMhs.get("sentAt")).isNotNull();
        assertThat(sentToMhs.get("taskId")).isNotNull();
    }

    private Document fetchSentToMhsForDocuments(Document ehrExtractStatus) {
        var gpcAccessDocument = ehrExtractStatus.get(GPC_ACCESS_DOCUMENT, Document.class);
        var documentList = gpcAccessDocument.get("documents", List.class);
        if (!documentList.isEmpty()) {
            return (Document) ((Document) documentList.get(0)).get("sentToMhs");
        }
        throw new IllegalStateException();
    }

    private String buildEhrExtractRequest(String conversationId, String notExistingPatientNhsNumber, String fromODSCode) throws IOException {
        return IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE)), StandardCharsets.UTF_8)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId)
            .replace(NHS_NUMBER_PLACEHOLDER, notExistingPatientNhsNumber)
            .replace(FROM_ODS_CODE_PLACEHOLDER, fromODSCode);
    }

    private String buildEhrExtractRequestWithoutNhsNumber(String conversationId, String fromODSCode) throws IOException {
        return IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(EHR_EXTRACT_REQUEST_WITHOUT_NHS_NUMBER_TEST_FILE)), StandardCharsets.UTF_8)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId)
            .replace(FROM_ODS_CODE_PLACEHOLDER, fromODSCode);
    }

    private List<Document> theDocumentTaskUpdatesTheRecord(Document ehrExtractStatus) {
        var gpcAccessDocument = ehrExtractStatus.get(GPC_ACCESS_DOCUMENT, Document.class);
        return gpcAccessDocument.get("documents", Collections.emptyList())
            .stream()
            .map(Document.class::cast)
            .filter(document -> document.get("objectName") != null)
            .collect(Collectors.toList());
    }

    private List<List<Object>> getTheSplitDocumentIds(Document ehrExtractStatus) {
        return theDocumentTaskUpdatesTheRecord(ehrExtractStatus).stream()
            .map(Document.class::cast)
            .map(document -> {
                var sentToMhs = document.get(SENT_TO_MHS);
                if (sentToMhs != null) {
                    return ((Document) sentToMhs).get(MESSAGE_ID, Collections.emptyList());
                }
                return Collections.emptyList();
            })
            .collect(Collectors.toList());
    }

    private void assertThatAcknowledgementPending(Document ackToRequester, String typeCode) {
        softly.assertThat(ackToRequester.get("messageId")).isNotNull();
        softly.assertThat(ackToRequester.get("taskId")).isNotNull();
        softly.assertThat(ackToRequester.get("typeCode")).isEqualTo(typeCode);
    }

    private void assertThatNegativeAcknowledgementToRequesterWasSent(Document ackToRequester, String typeCode) {
        assertThatAcknowledgementPending(ackToRequester, typeCode);
        softly.assertThat(ackToRequester.get("reasonCode")).isEqualTo(NACK_CODE_PATIENT_NOT_FOUND);
        softly.assertThat(ackToRequester.get("detail")).isEqualTo(NACK_MESSAGE_NOT_FOUND);
    }

    private void assertThatNoErrorInfoIsStored(String conversationId) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");
        assertThat(error).isNull();
    }

    private void assertThatErrorInfoIsStored(String conversationId, String expectedTaskType) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");

        softly.assertThat(error.get("occurredAt")).isNotNull();
        softly.assertThat(error.get("code")).isEqualTo(NACK_CODE_PATIENT_NOT_FOUND);
        softly.assertThat(error.get("message")).isEqualTo(NACK_MESSAGE_NOT_FOUND);
        softly.assertThat(error.get("taskType")).isEqualTo(expectedTaskType);
    }

    private void assertThatExtractContinueMessageWasSent(Document ehrContinue) {
        softly.assertThat(ehrContinue).isNotEmpty().isNotEmpty();
        softly.assertThat(ehrContinue.get("received")).isNotNull();
    }

    private void assertHappyPathWithDocs(String conversationId, String fromODSCode, String nhsNumber) {
        var ehrExtractStatus = waitFor(() -> getFinishedEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, fromODSCode);

        var gpcAccessStructured = ehrExtractStatus.get(GPC_ACCESS_STRUCTURED, Document.class);
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        theDocumentTaskUpdatesTheRecord(ehrExtractStatus)
            .forEach(this::assertThatAccessDocumentWasFetched);

        var ehrExtractCore = ehrExtractStatus.get(EHR_EXTRACT_CORE, Document.class);
        assertThatExtractCoreMessageWasSent(ehrExtractCore);

        var ehrContinue = ehrExtractStatus.get(EHR_CONTINUE, Document.class);
        assertThatExtractContinueMessageWasSent(ehrContinue);

        var ackPending = ehrExtractStatus.get(ACK_TO_PENDING, Document.class);
        assertThatAcknowledgementPending(ackPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);

        var sentToMhs = fetchSentToMhsForDocuments(ehrExtractStatus);
        assertThat(sentToMhs.get("messageId")).isNotNull();
        assertThat(sentToMhs.get("sentAt")).isNotNull();
        assertThat(sentToMhs.get("taskId")).isNotNull();
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

    private void assertThatAccessDocumentWasFetched(Document document) {
        softly.assertThat(document.get("accessedAt")).isNotNull();
        softly.assertThat(document.get("taskId")).isNotNull();
    }

    private void assertThatAccessAbsentDocumentWasFetched(Document document) {
        softly.assertThat(nameStartsEndsWith(document.get("objectName"), "AbsentAttachment", ".txt")).isEqualTo(true);
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

    private boolean nameStartsEndsWith(Object name, String startingValue, String endingValue){
        return nameStartsWith(name.toString(), startingValue)
            && nameEndsWith(name.toString(), endingValue);
    }

    private boolean nameStartsWith(String name, String startingValue) {
        return name.startsWith(startingValue);
    }

    private boolean nameEndsWith(String name, String endingValue) {
        return name.endsWith(endingValue);
    }

    private void checkNhsNumberTriggersNackWithCode(String nackCode, String NhsNumber) throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NhsNumber, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var requestJournal = waitFor(() -> {
            try {
                return mhsMockRequestsJournal.getRequestsJournal(conversationId);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(requestJournal).hasSize(1);
        XmlAssert.assertThat(requestJournal.get(0).getPayload())
            .hasXPath("/MCCI_IN010000UK13/acknowledgement[@type='Acknowledgement' and @typeCode='AE']/acknowledgementDetail[@type='AcknowledgementDetail' and @typeCode='ER']/code[@code='%nackCode%']"
                .replace("/", XML_NAMESPACE).replace("%nackCode%", nackCode));
        XmlAssert.assertThat(requestJournal.get(0).getPayload())
            .hasXPath("/MCCI_IN010000UK13/ControlActEvent/reason/justifyingDetectedIssueEvent/code[@code='%nackCode%']"
                .replace("/", XML_NAMESPACE).replace("%nackCode%", nackCode));
    }
}
