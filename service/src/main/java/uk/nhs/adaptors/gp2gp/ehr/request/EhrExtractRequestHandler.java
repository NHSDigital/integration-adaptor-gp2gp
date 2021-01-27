package uk.nhs.adaptors.gp2gp.ehr.request;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.exception.MissingValueException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

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

    // FIXME: Remove these constants as part of NIAD-814
    private static final String DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    private static final String DOCUMENT_URL = "https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/fhir/Binary/" + DOCUMENT_ID;

    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final XPathService xPathService;
    private final TimestampService timestampService;
    private final TaskDispatcher taskDispatcher;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final MongoTemplate mongoTemplate; // FIXME: Remove as part of NIAD-814

    public void handle(Document header, Document payload) {
        var ehrExtractStatus = prepareEhrExtractStatus(header, payload);
        if (saveNewExtractStatusDocument(ehrExtractStatus)) {
            LOGGER.info("Creating tasks to start the EHR Extract process");
            createGetGpcStructuredTask(ehrExtractStatus);
            createGetGpcDocumentTask(ehrExtractStatus);
            createSendEhrExtractCoreMessage(ehrExtractStatus);
        } else {
            LOGGER.info("Skipping creation of new tasks for the duplicate extract request");
        }
    }

    private EhrExtractStatus prepareEhrExtractStatus(Document header, Document payload) {
        EhrExtractStatus.EhrRequest ehrRequest = prepareEhrRequest(header, payload);
        Instant now = timestampService.now();
        String conversationId = xPathService.getNodeValue(header, CONVERSATION_ID_PATH);
        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(conversationId)
            .ehrRequest(ehrRequest)
            .build();
    }

    private boolean saveNewExtractStatusDocument(EhrExtractStatus ehrExtractStatus) {
        try {
            ehrExtractStatusRepository.save(ehrExtractStatus);
            LOGGER.info("An ehr_extract_status document was added to the database for the extract request");
            return true;
        } catch (DuplicateKeyException e) {
            LOGGER.warn("A duplicate extract request was received and ignored");
            return false;
        }
    }

    private void createGetGpcStructuredTask(EhrExtractStatus ehrExtractStatus) {
        var getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .build();
        taskDispatcher.createTask(getGpcStructuredTaskDefinition);
    }

    // FIXME: move/remove NIAD-814 should create a task for each of the patient's documents
    private void createGetGpcDocumentTask(EhrExtractStatus ehrExtractStatus) {
        addAccessDocument(ehrExtractStatus.getConversationId());

        var getGpcDocumentTaskTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .documentId(DOCUMENT_ID)
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .accessDocumentUrl(DOCUMENT_URL)
            .build();
        taskDispatcher.createTask(getGpcDocumentTaskTaskDefinition);
    }

    @Deprecated // FIXME: Creates a stub db element that will be added by NIAD-814. Remove as part of NIAD-814
    private void addAccessDocument(String conversationId) {
        org.bson.Document document = new org.bson.Document();
        document.append("documentId", DOCUMENT_ID);
        document.append("accessDocumentUrl", DOCUMENT_URL);

        var collection = mongoTemplate.getCollection("ehrExtractStatus");
        collection.updateOne(Filters.eq("conversationId", conversationId), Updates.addToSet("gpcAccessDocument.documents", document));
    }

    private void createSendEhrExtractCoreMessage(EhrExtractStatus ehrExtractStatus) {
        var sendEhrExtractCoreTaskDefinition = SendEhrExtractCoreTaskDefinition.builder()
                .taskId(taskIdService.createNewTaskId())
                .conversationId(ehrExtractStatus.getConversationId())
                .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
                .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
                .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
                .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
                .build();
        taskDispatcher.createTask(sendEhrExtractCoreTaskDefinition);
    }

    private EhrExtractStatus.EhrRequest prepareEhrRequest(Document header, Document payload) {
        return new EhrExtractStatus.EhrRequest(
            getRequiredValue(payload, REQUEST_ID_PATH),
            getRequiredValue(payload, NHS_NUMBER_PATH),
            getRequiredValue(header, FROM_PARTY_ID_PATH),
            getRequiredValue(header, TO_PARTY_ID_PATH),
            getRequiredValue(payload, FROM_ASID_PATH),
            getRequiredValue(payload, TO_ASID_PATH),
            getRequiredValue(payload, FROM_ODS_CODE_PATH),
            getRequiredValue(payload, TO_ODS_CODE_PATH)
        );
    }

    private String getRequiredValue(Document xml, String xpath) {
        String value = xPathService.getNodeValue(xml, xpath);
        if (StringUtils.isBlank(value)) {
            throw MissingValueException.builder()
                .interaction(SpineInteraction.EHR_EXTRACT_REQUEST)
                .xpath(xpath)
                .build();
        }
        return value;
    }
}
