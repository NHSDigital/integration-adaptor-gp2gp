package uk.nhs.adaptors.gp2gp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.xmlunit.assertj.XmlAssert;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.MessageQueue;
import uk.nhs.adaptors.gp2gp.Mongo;

import java.util.Arrays;
import java.util.stream.Collectors;

@ExtendWith(SoftAssertionsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class EhrExtractTest {
    @InjectSoftAssertions
    private SoftAssertions softly;

    //AA-DR = AbsentAttachment DocumentReference
    //NORMAL-DR = Non-AbsentAttachment DocumentReference
    private static final String NHS_NUMBER_WITH_AA_DR = "9690937286";
    private static final String NHS_NUMBER_WITH_NORMAL_DR = "9690937287";
    private static final String NHS_NUMBER_NOT_EXISTING_PATIENT = "9876543210";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_1 = "9690937819";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_2 = "9690937841";
    private static final String NHS_NUMBER_TWO_DOCUMENTS = "9690937789";
    private static final String NHS_NUMBER_THREE_SMALL_AA_DOCUMENTS = "9690937419";
    private static final String NHS_NUMBER_THREE_SMALL_NORMAL_DOCUMENTS = "9690937420";
    private static final String NHS_NUMBER_LARGE_PAYLOAD = "9690937421";
    private static final String NHS_NUMBER_LARGE_ATTACHMENT_DOCX = "9388098434";

    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String EHR_EXTRACT_REQUEST_NO_DOCUMENTS_TEST_FILE = "/ehrExtractRequestWithNoDocuments.json";
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

    private static final String DOCUMENT_REFERENCE_XPATH_TEMPLATE = "/EhrExtract/component/ehrFolder/component/ehrComposition/component/NarrativeStatement/reference/referredToExternalDocument/text/reference[@value='file://localhost/%s']";

    private final MhsMockRequestsJournal mhsMockRequestsJournal =
        new MhsMockRequestsJournal(getEnvVar("GP2GP_MHS_MOCK_BASE_URL", "http://localhost:8081"));

    @BeforeEach
    void setUp() {
        mhsMockRequestsJournal.deleteRequestsJournal();
    }

    @AfterEach
    void cleanDb() {
        boolean isClear = waitFor(Mongo::clearDb);
        Document d = waitFor(Mongo::getStats);

        log.info("Database cleared after test: {}", isClear);
        log.info("Database document count: {}", d.get("count"));
    }

    @Test
    @Order(1)
    public void When_ExtractRequestReceivedWithLargeAttachment_Expect_LargeDocumentIsSplit() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_ATTACHMENT_DOCX, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithDocs(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_LARGE_ATTACHMENT_DOCX);
    }

    @Test
    @Order(2)
    public void When_ExtractRequestReceived3NormalDocs_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_THREE_SMALL_NORMAL_DOCUMENTS, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithDocs(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_THREE_SMALL_NORMAL_DOCUMENTS);
        assertMultipleDocumentsRetrieved(conversationId, 3);
    }
    @Test
    @Order(3)
    public void When_ExtractRequestReceivedForLargeEhrExtract_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_WITH_AA_DR, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertEhrExtractSentAsAttachment(conversationId);

        assertHappyPathWithAbsentAttachments(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_WITH_AA_DR);
    }

    @Test
    public void When_ExtractRequestReceivedFromNormal_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_WITH_NORMAL_DR, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithDocs(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_WITH_NORMAL_DR);
    }

    @Test
    public void When_LargeExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_PAYLOAD, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithDocs(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_LARGE_PAYLOAD);
    }

    @Test
    public void When_ExtractRequestReceived_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_WITH_AA_DR, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithAbsentAttachments(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_WITH_AA_DR);

        String conversationId2 = UUID.randomUUID().toString();
        String ehrExtractRequest2 = buildEhrExtractRequest(conversationId2, NHS_NUMBER_WITH_AA_DR, FROM_ODS_CODE_2);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest2);

        assertHappyPathWithAbsentAttachments(conversationId2, FROM_ODS_CODE_2, NHS_NUMBER_WITH_AA_DR);
    }

    @Test
    public void When_ExtractRequestReceivedForPatientWith2Docs1Large_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_TWO_DOCUMENTS, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithAbsentAttachments(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_TWO_DOCUMENTS);
        assertMultipleDocumentsRetrieved(conversationId, 2);
    }

    @Test
    public void When_ExtractRequestReceived3AbsentAttachmentDocs_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_THREE_SMALL_AA_DOCUMENTS, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithAbsentAttachments(conversationId, FROM_ODS_CODE_1, NHS_NUMBER_THREE_SMALL_AA_DOCUMENTS);
        assertMultipleDocumentsRetrieved(conversationId, 3);
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

        var documentList = waitFor(() -> {
            var extractStatus = ((Document) Mongo.findEhrExtractStatus(conversationId)
                .get(GPC_ACCESS_DOCUMENT));
            if (extractStatus == null) {
                return null;
            }
            return extractStatus.get("documents", Collections.emptyList());
        });

        assertThat(documentList.size()).isEqualTo(0);

        var ackToPending = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_PENDING));
        assertThatAcknowledgementPending(ackToPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);
        assertThatNoErrorInfoIsStored(conversationId);

        var mhsMockRequests = mhsMockRequestsJournal.getRequestsJournal();
        assertThat(mhsMockRequests).hasSize(2);
        var ehrExtractMhsRequest = mhsMockRequests.get(0);

        assertThat(ehrExtractMhsRequest.getAttachments()).hasSize(1);
        assertThat(ehrExtractMhsRequest.getExternalAttachments()).hasSize(0);

        var payload = ehrExtractMhsRequest.getPayload();
        var attachment = ehrExtractMhsRequest.getAttachments().get(0);

        assertThat(attachment.getPayload()).isNotBlank();
        assertThat(attachment.getContentType()).isEqualTo("application/xml");
        assertThat(attachment.getIsBase64()).isEqualTo("false");

        var description = attachment.getDescription();
        var descriptionElements = Arrays.stream(description.split("\n"))
            .filter(StringUtils::isNotBlank)
            .map(value -> value.split("="))
            .collect(Collectors.toMap(x -> x[0].trim(), x -> x[1]));

        assertThat(descriptionElements).containsEntry("ContentType", "application/xml");
        assertThat(descriptionElements).containsEntry("Compressed", "Yes");
        assertThat(descriptionElements).containsEntry("LargeAttachment", "Yes");
        assertThat(descriptionElements).containsEntry("OriginalBase64", "No");
        assertThat(descriptionElements).hasEntrySatisfying("Length", lengthAsString -> {
            var lengthAsInt = Integer.parseInt(lengthAsString);
            assertThat(lengthAsInt).isGreaterThan(0);
        });
        assertThat(descriptionElements).containsEntry("DomainData", "X-GP2GP-Skeleton: Yes");
        assertThat(descriptionElements).containsKey("Filename");
        var fileName = descriptionElements.get("Filename");

        var documentReferenceXPath = String.format(DOCUMENT_REFERENCE_XPATH_TEMPLATE, fileName);
        XmlAssert.assertThat(payload).hasXPath(documentReferenceXPath);
    }

    @Test
    public void When_ExtractRequestReceivedForNotExistingPatient_Expect_ErrorUpdatedInDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_NOT_EXISTING_PATIENT, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, NHS_NUMBER_NOT_EXISTING_PATIENT, FROM_ODS_CODE_1);

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

        assertMultipleDocsSent(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_1, 1);
    }

    @Test
    public void When_ExtractRequestReceivedWithDocumentSizeLargerThanThreshold_Expect_LargeDocumentIsSplit() throws Exception {
        // file size: 62428
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, FROM_ODS_CODE_1);

        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertMultipleDocsSent(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, 3);
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

        assertThat(documentList.size()).isEqualTo(documentCount);
    }

    private void assertMultipleDocsSent(String conversationId, String nhsNumber, int arraySize) {
        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, FROM_ODS_CODE_1);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessDocumentWasFetched(singleDocument);

        var messageIds = waitFor(() -> getTheSplitDocumentIds(conversationId));
        softly.assertThat(messageIds.size()).isEqualTo(arraySize);
    }

    private void assertHappyPathWithAbsentAttachments(String conversationId, String fromODSCode, String nhsNumber) {
        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, fromODSCode);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);
        
        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessAbsentDocumentWasFetched(singleDocument);

        var ehrExtractCore = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_EXTRACT_CORE));
        assertThatExtractCoreMessageWasSent(ehrExtractCore);

        var ehrContinue = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_CONTINUE));
        assertThatExtractContinueMessageWasSent(ehrContinue);

        var ackPending = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_PENDING));
        assertThatAcknowledgementPending(ackPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);

        var sentToMhs = (Document) waitFor(() -> fetchSentToMhsForDocuments(conversationId));
        assertThat(sentToMhs.get("messageId")).isNotNull();
        assertThat(sentToMhs.get("sentAt")).isNotNull();
        assertThat(sentToMhs.get("taskId")).isNotNull();
    }

    private void assertEhrExtractSentAsAttachment(String conversationId) {
        var gpcAccessStructured = waitFor(() -> accessStructuredWithAttachmentThatHasBeenSent(conversationId));
        assertThat(gpcAccessStructured.get("documentId")).isNotNull();
        assertThat(gpcAccessStructured.get("objectName")).isNotNull();
        assertThat(gpcAccessStructured.get("accessedAt")).isNotNull();
        assertThat(gpcAccessStructured.get("taskId")).isNotNull();
        assertThat(gpcAccessStructured.get("messageId")).isNotNull();
        assertThat(gpcAccessStructured.get("sentToMhs")).isNotNull();
    }

    private Document fetchSentToMhsForDocuments(String conversationId) {
        var gpcAccessDocument = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_DOCUMENT);
        if (gpcAccessDocument != null && gpcAccessDocument.get("documents", Collections.emptyList()) != null ) {
            var documentList = gpcAccessDocument.get("documents", Collections.emptyList());
            if (!documentList.isEmpty()) {
                return (Document) ((Document) documentList.get(0)).get("sentToMhs");
            }
        }
        return null;
    }

    private Document accessStructuredWithAttachmentThatHasBeenSent(String conversationId) {
        var gpcAccessStructured = (Document) Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED);
        if (gpcAccessStructured != null) {
            var attachment = (Document) gpcAccessStructured.get("attachment");
            if (attachment != null && attachment.get("sentToMhs") != null) {
                return attachment;
            }
        }
        return null;
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

    private void assertHappyPathWithDocs(String conversationId, String fromODSCode, String nhsNumber) {
        var ehrExtractStatus = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
        assertThatInitialRecordWasCreated(conversationId, ehrExtractStatus, nhsNumber, fromODSCode);

        var gpcAccessStructured = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(GPC_ACCESS_STRUCTURED));
        assertThatAccessStructuredWasFetched(conversationId, gpcAccessStructured);

        var singleDocument = (Document) waitFor(() -> theDocumentTaskUpdatesTheRecord(conversationId));
        assertThatAccessDocumentWasFetched(singleDocument);

        var ehrExtractCore = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_EXTRACT_CORE));
        assertThatExtractCoreMessageWasSent(ehrExtractCore);

        var ehrContinue = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(EHR_CONTINUE));
        assertThatExtractContinueMessageWasSent(ehrContinue);

        var ackPending = (Document) waitFor(() -> Mongo.findEhrExtractStatus(conversationId).get(ACK_TO_PENDING));
        assertThatAcknowledgementPending(ackPending, ACCEPTED_ACKNOWLEDGEMENT_TYPE_CODE);

        var sentToMhs = (Document) waitFor(() -> fetchSentToMhsForDocuments(conversationId));
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
        softly.assertThat(nameEndsWith(document.get("objectName").toString(), ".json")).isEqualTo(true);

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

}
