package uk.nhs.adaptors.gp2gp.e2e;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.xmlunit.assertj.XmlAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
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
    private static final String NEGATIVE_ACKNOWLEDGEMENT_TYPE_CODE = "AE";
    private static final String GET_GPC_STRUCTURED_TASK_NAME = "GET_GPC_STRUCTURED";

    private static final String NHS_NUMBER = "9690937286";
    private static final String NHS_NUMBER_NO_DOCUMENTS = "9690937294";
    private static final String NHS_NUMBER_NOT_FOUND = "9876543210";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_1 = "9690937819";
    private static final String NHS_NUMBER_LARGE_DOCUMENTS_2 = "9690937841";

    private static final String FROM_ODS_CODE_1 = "GPC001";
    private static final String FROM_ODS_CODE_2 = "B2617";
    private static final String DOCUMENT_ID_NORMAL = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String DOCUMENT_ID_LARGE = "11737b22-8cff-47e2-b741-e7f27c8c61a8";
    private static final String DOCUMENT_ID_LARGE_2 = "29c434d6-ad47-415f-b5f5-fd1dc2941d8d";

    private static final String DOCUMENT_REFERENCE_XPATH_TEMPLATE = "/EhrExtract/component/ehrFolder/component/ehrComposition/component/NarrativeStatement/reference/referredToExternalDocument/text/reference[@value='file://localhost/%s']";

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
        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
                .numberOfMessages(6)
                .numberOAttachments(0)
                .numberOfExternalAttachments(2)
                .build()
        );

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

        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
            .numberOfMessages(2)
            .numberOAttachments(1)
            .numberOfExternalAttachments(0)
            .compressedEhr(true)
            .build()
        );
    }

    @Test
    public void When_ExtractRequestReceivedForNotExistingPatient_Expect_EhrStatusToBeUpdatedWithFailure() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_NOT_FOUND, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertInitialRecordCreated(conversationId, NHS_NUMBER_NOT_FOUND, FROM_ODS_CODE_1);
        assertThatAcknowledgementSentToRequester(conversationId, NEGATIVE_ACKNOWLEDGEMENT_TYPE_CODE);
        assertThatErrorInfoIsStored(conversationId);
        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
            .numberOfMessages(1)
            .build()
        );
    }

    // chunking flows
    // test ehrExtract large docs
    @Test
    public void When_ExtractRequestReceivedWithDocumentSizeEqualThreshold_Expect_LargeDocumentIsSentAsOne() throws Exception {
        // file size: 31216
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_1, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithoutDocuments(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_1, FROM_ODS_CODE_1);
        assertThatFirstDocumentWasFetched(conversationId, DOCUMENT_ID_LARGE);
        assertThatDocumentsWasSentInParts(conversationId, 1);
        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
                .compressedEhr(true)
                .numberOfMessages(3)
                .numberOfExternalAttachments(1)
                .numberOAttachments(0)
                .build()
        );
    }

    @Test
    public void When_ExtractRequestReceivedWithDocumentSizeLargerThanThreshold_Expect_LargeDocumentIsSplit() throws Exception {
        // file size: 62428
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithoutDocuments(conversationId, NHS_NUMBER_LARGE_DOCUMENTS_2, FROM_ODS_CODE_1);
        assertThatFirstDocumentWasFetched(conversationId, DOCUMENT_ID_LARGE_2);
        assertThatDocumentsWasSentInParts(conversationId, 3);
        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
                .compressedEhr(true)
                .numberOfMessages(5)
                .numberOfExternalAttachments(1)
                .numberOAttachments(0)
                .build()
        );
    }

    @Test
    public void When_ExtractRequestReceivedForLargeEhrExtract_Expect_ExtractStatusAndDocumentDataAddedToDatabase() throws Exception {
        String conversationId = UUID.randomUUID().toString();
        String ehrExtractRequest = buildEhrExtractRequest(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        MessageQueue.sendToMhsInboundQueue(ehrExtractRequest);

        assertHappyPathWithoutDocuments(conversationId, NHS_NUMBER, FROM_ODS_CODE_1);
        assertThatFirstDocumentWasFetched(conversationId, DOCUMENT_ID_NORMAL);
        assertMessagesWereSentToMhs(
            MhsMessageAssertions.builder()
                .compressedEhr(true)
                .numberOfMessages(6)
                .numberOfExternalAttachments(2)
                .numberOAttachments(0)
                .build()
        );
    }

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

    private void assertThatErrorInfoIsStored(String conversationId) {
        var error = (Document) Mongo.findEhrExtractStatus(conversationId).get("error");

        softly.assertThat(error.get("occurredAt")).isNotNull();
        softly.assertThat(error.get("code")).isEqualTo("18");
        softly.assertThat(error.get("message")).isEqualTo("An error occurred when executing a task");
        softly.assertThat(error.get("taskType")).isEqualTo(GET_GPC_STRUCTURED_TASK_NAME);
    }

    private void assertThatDocumentsWasSentInParts(String conversationId, int arraySize) {
        var document = waitFor(() -> fetchFirstObjectFromList(conversationId, GPC_ACCESS_DOCUMENT, DOCUMENTS));
        var sentToMhs = (Document)document.get("sentToMhs");

        softly.assertThat(sentToMhs).isNotNull();
        var documentIds = sentToMhs.get(MESSAGE_ID.toString(), Collections.emptyList());
        softly.assertThat(documentIds).isNotNull();
        softly.assertThat(documentIds.size()).isEqualTo(arraySize);

    }

    // TODO: 08/09/2021 add mhsAssertions to other tests
    private void assertMessagesWereSentToMhs(MhsMessageAssertions messageAssertions) throws InterruptedException, IOException {
        var mhsMockRequests = mhsMockRequestsJournal.getRequestsJournal();
        assertThat(mhsMockRequests).hasSize(messageAssertions.numberOfMessages);

        var ehrExtractMhsRequest = mhsMockRequests.get(0);
        if (messageAssertions.numberOAttachments > 0)
            assertThat(ehrExtractMhsRequest.getAttachments()).hasSize(messageAssertions.numberOAttachments);
        if (messageAssertions.numberOfExternalAttachments > 0)
            assertThat(ehrExtractMhsRequest.getExternalAttachments()).hasSize(messageAssertions.numberOfExternalAttachments);

        // TODO: 08/09/2021 make non compressed ehrExtractVersion
        if (messageAssertions.compressedEhr && messageAssertions.getNumberOAttachments() > 0) {
            var payload = ehrExtractMhsRequest.getPayload();
            var attachment = ehrExtractMhsRequest.getAttachments().get(0);

            assertThat(attachment.getPayload()).isNotBlank();
            assertThat(attachment.getContentType()).isEqualTo("application/xml");
            assertThat(attachment.getIsBase64()).isEqualTo("false");

            var description = attachment.getDescription();
            var descriptionElements = splitDescriptionElement(description);

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
    }

    private Map<String, String> splitDescriptionElement(String description) {
        return Arrays.stream(description.split("\n"))
            .filter(StringUtils::isNotBlank)
            .map(value -> value.split("="))
            .collect(Collectors.toMap(x -> x[0].trim(), x -> x[1]));
    }

    @Builder
    @Getter
    private static class MhsMessageAssertions {
        private final int numberOfMessages;
        private final int numberOAttachments;
        private final int numberOfExternalAttachments;
        @Builder.Default
        private final boolean compressedEhr = false;
        @Builder.Default
        private final boolean splitCompressedEhr = false;
    }
}
