package uk.nhs.adaptors.gp2gp.ehr.request;

import static java.time.Instant.now;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.MissingValueException;
import uk.nhs.adaptors.gp2gp.ehr.SpineInteraction;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GpcConfiguration;

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

    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final XPathService xPathService;
    private final TimestampService timestampService;
    private final TaskDispatcher taskDispatcher;
    private final TaskIdService taskIdService;
    private final GpcConfiguration gpcConfiguration;

    public void handle(Document header, Document payload) {
        var ehrExtractStatus = prepareEhrExtractStatus(header, payload);
        if (saveNewExtractStatusDocument(ehrExtractStatus)) {
            LOGGER.info("Creating tasks to start the EHR Extract process");
            createGetGpcStructuredTask(ehrExtractStatus);
        } else {
            LOGGER.info("Skipping creation of new tasks for the duplicate extract request");
        }
    }

    private EhrExtractStatus prepareEhrExtractStatus(Document header, Document payload) {
        EhrExtractStatus.EhrRequest ehrRequest = prepareEhrRequest(header, payload);
        Instant now = timestampService.now();
        String conversationId = xPathService.getNodeValue(header, CONVERSATION_ID_PATH);
        return new EhrExtractStatus(now, now, conversationId, ehrRequest);
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
            .taskId(taskIdService.createNewTaskId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .build();
        taskDispatcher.createTask(getGpcStructuredTaskDefinition);
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

    public EhrExtractStatus getEhrExtractStatus(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Optional<EhrExtractStatus> ehrStatus =
            ehrExtractStatusRepository.findByConversationId(structuredTaskDefinition.getConversationId());
        if (ehrStatus.isPresent()) {
            return ehrStatus.get();
        } else {
            throw new RuntimeException("No EHR Extract Status for ConversationId: " + structuredTaskDefinition.getConversationId());
        }
    }

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition,
        EhrExtractStatus ehrExtractStatus) {
        var accessStructured = new EhrExtractStatus.GpcAccessStructured(ehrExtractStatus.getConversationId()
            + "_gpc_structured.json", now(),
            structuredTaskDefinition.getTaskId());
        ehrExtractStatus.setGpcAccessStructured(accessStructured);
        ehrExtractStatus.setUpdatedAt(now());
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }
}
