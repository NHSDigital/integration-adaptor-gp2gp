package uk.nhs.adaptors.gp2gp.common.task;

import java.time.Instant;
import java.util.UUID;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
public class EhrStatusConsumerService {
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

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    public void handleEhrStatus(Document ebXmlDocument, Document payloadDocument) {
        EhrExtractStatus ehrExtractStatus = prepareEhrExtractStatus(ebXmlDocument, payloadDocument);
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    public EhrExtractStatus prepareEhrExtractStatus(Document ebXmlDocument, Document payloadDocument) {
        EhrExtractStatus.EhrRequest ehrRequest = prepareEhrRequest(ebXmlDocument, payloadDocument);

        Instant now = Instant.now();

        return new EhrExtractStatus(UUID.randomUUID().toString(),
            now,
            now,
            XPathService.getNodeValue(ebXmlDocument, CONVERSATION_ID_PATH),
            ehrRequest);
    }

    private EhrExtractStatus.EhrRequest prepareEhrRequest(Document ebXmlDocument, Document payloadDocument) {
        return new EhrExtractStatus.EhrRequest(
            XPathService.getNodeValue(payloadDocument, REQUEST_ID_PATH),
            XPathService.getNodeValue(payloadDocument, NHS_NUMBER_PATH),
            XPathService.getNodeValue(ebXmlDocument, FROM_PARTY_ID_PATH),
            XPathService.getNodeValue(ebXmlDocument, TO_PARTY_ID_PATH),
            XPathService.getNodeValue(payloadDocument, FROM_ASID_PATH),
            XPathService.getNodeValue(payloadDocument, TO_ASID_PATH),
            XPathService.getNodeValue(payloadDocument, FROM_ODS_CODE_PATH),
            XPathService.getNodeValue(payloadDocument, TO_ODS_CODE_PATH)
        );
    }
}
