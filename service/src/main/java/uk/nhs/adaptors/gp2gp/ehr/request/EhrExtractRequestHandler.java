package uk.nhs.adaptors.gp2gp.ehr.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.exception.MissingValueException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractRequestHandler {
    private static final String INTERACTION_ID_PATH = "/RCMR_IN010000UK05";
    private static final String SUBJECT_PATH = INTERACTION_ID_PATH + "/ControlActEvent/subject";
    private static final String MESSAGE_HEADER_PATH = "/Envelope/Header/MessageHeader";
    private static final String CONVERSATION_ID_PATH = MESSAGE_HEADER_PATH + "/ConversationId";
    private static final String REQUEST_ID_PATH = SUBJECT_PATH + "/EhrRequest/id/@root";
    private static final String NHS_NUMBER_PATH = SUBJECT_PATH + "/EhrRequest/recordTarget/patient/id/@extension";
    private static final String FROM_PARTY_ID_PATH = MESSAGE_HEADER_PATH + "/From/PartyId";
    private static final String TO_PARTY_ID_PATH = MESSAGE_HEADER_PATH + "/To/PartyId";
    private static final String FROM_ASID_PATH = INTERACTION_ID_PATH + "/communicationFunctionSnd/device/id/@extension";
    private static final String TO_ASID_PATH = INTERACTION_ID_PATH + "/communicationFunctionRcv/device/id/@extension";
    private static final String FROM_ODS_CODE_PATH = SUBJECT_PATH + "/EhrRequest/author/AgentOrgSDS/agentOrganizationSDS/id/@extension";
    private static final String TO_ODS_CODE_PATH = SUBJECT_PATH + "/EhrRequest/destination/AgentOrgSDS/agentOrganizationSDS/id/@extension";
    private static final String CONTINUE_ACKNOWLEDGEMENT = "Continue Acknowledgement";
    private static final String MESSAGE_ID_PATH = MESSAGE_HEADER_PATH + "/MessageData/MessageId";

    private final EhrExtractStatusService ehrExtractStatusService;
    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final XPathService xPathService;
    private final TimestampService timestampService;
    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final EhrExtractAckHandler ackHandler;

    public void handleStart(Document header, Document payload, Instant messageTimestamp) {
        var ehrExtractStatus = prepareEhrExtractStatus(header, payload, messageTimestamp);
        ehrExtractStatus = saveExtractStatusDocument(ehrExtractStatus);
        if (ehrExtractStatus != null) {
            addAdditionalInformationToEhrExtract(ehrExtractStatus.getEhrRequest(), header, payload);
            ehrExtractStatus = saveExtractStatusDocument(ehrExtractStatus);
            if (ehrExtractStatus == null) {
                throw new IllegalStateException("Unable to save updated ehr extract status");
            }
            LOGGER.info("Creating tasks to start the EHR Extract process");
            createGetGpcStructuredTask(ehrExtractStatus);
        } else {
            LOGGER.info("Skipping creation of new tasks for the duplicate extract request");
        }
    }

    private void addAdditionalInformationToEhrExtract(EhrExtractStatus.EhrRequest ehrRequest, Document header, Document payload) {
        ehrRequest.setNhsNumber(getRequiredValue(payload, NHS_NUMBER_PATH));
        ehrRequest.setFromPartyId(getRequiredValue(header, FROM_PARTY_ID_PATH));
        ehrRequest.setToPartyId(getRequiredValue(header, TO_PARTY_ID_PATH));
        ehrRequest.setToOdsCode(getRequiredValue(payload, TO_ODS_CODE_PATH));
    }

    private EhrExtractStatus prepareEhrExtractStatus(Document header, Document payload, Instant messageTimestamp) {
        var ehrRequest = prepareMinimalEhrRequest(header, payload);
        var now = timestampService.now();
        var conversationId = getRequiredValue(header, CONVERSATION_ID_PATH);
        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(conversationId)
            .ehrRequest(ehrRequest)
            .messageTimestamp(messageTimestamp)
            .build();
    }

    private EhrExtractStatus saveExtractStatusDocument(EhrExtractStatus ehrExtractStatus) {
        try {
            var savedEhrExtractStatus = ehrExtractStatusRepository.save(ehrExtractStatus);
            LOGGER.info("An ehr_extract_status document was added to the database for the extract request");
            return savedEhrExtractStatus;
        } catch (DuplicateKeyException e) {
            LOGGER.warn("A duplicate extract request was received and ignored");
            return null;
        }
    }

    private void createGetGpcStructuredTask(EhrExtractStatus ehrExtractStatus) {
        var getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.getGetGpcStructuredTaskDefinition(randomIdGeneratorService,
                                                                                                              ehrExtractStatus);
        taskDispatcher.createTask(getGpcStructuredTaskDefinition);
    }



    private EhrExtractStatus.EhrRequest prepareMinimalEhrRequest(Document header, Document payload) {
        return EhrExtractStatus.EhrRequest.builder()
            .messageId(getRequiredValue(header, MESSAGE_ID_PATH))
            .requestId(getRequiredValue(payload, REQUEST_ID_PATH))
            .fromAsid(getRequiredValue(payload, FROM_ASID_PATH))
            .toAsid(getRequiredValue(payload, TO_ASID_PATH))
            .fromOdsCode(getRequiredValue(payload, FROM_ODS_CODE_PATH))
            .build();
    }

    private String getRequiredValue(Document xml, String xpath) {
        var value = xPathService.getNodeValue(xml, xpath);
        if (StringUtils.isBlank(value)) {
            throw MissingValueException.builder()
                .interaction(SpineInteraction.EHR_EXTRACT_REQUEST)
                .xpath(xpath)
                .build();
        }
        return value;
    }

    public void handleContinue(String conversationId, String payload) {
        if (payload.contains(CONTINUE_ACKNOWLEDGEMENT)) {
            ehrExtractStatusService.updateEhrExtractStatusContinue(conversationId)
                .ifPresent(ehrExtractStatus -> {
                    var documents = ehrExtractStatus.getGpcAccessDocument().getDocuments();
                    LOGGER.info("Sending documents for: ConversationId: " + conversationId);
                    for (int documentPosition = 0; documentPosition < documents.size(); documentPosition++) {
                        var document = documents.get(documentPosition);
                        createSendDocumentTasks(
                            ehrExtractStatus,
                            document.getObjectName(),
                            documentPosition,
                            document.getMessageId(),
                            document.getDocumentId(),
                            document.getContentType());
                    }
                });
        } else {
            throw new InvalidInboundMessageException("Continue Message did not have Continue Acknowledgment, conversationId: "
                + conversationId);
        }
    }

    private void createSendDocumentTasks(
            EhrExtractStatus ehrExtractStatus, String documentName, int documentLocation, String messageId,
            String documentId, String documentContentType
    ) {
        var sendDocumentTaskDefinition = SendDocumentTaskDefinition.builder()
            .documentName(documentName)
            .documentPosition(documentLocation)
            .documentContentType(documentContentType)
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .messageId(messageId)
            .documentId(documentId)
            .build();
        LOGGER.info("Sending task for document_id: {} document_name: {}", documentId, documentName);
        taskDispatcher.createTask(sendDocumentTaskDefinition);

    }

    public void handleAcknowledgement(String conversationId, Document payload) {
        ackHandler.handle(conversationId, payload);
    }
}
