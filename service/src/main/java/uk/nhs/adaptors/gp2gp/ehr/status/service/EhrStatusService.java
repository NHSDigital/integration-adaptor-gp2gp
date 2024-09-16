package uk.nhs.adaptors.gp2gp.ehr.status.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;


@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusService extends EhrStatusBaseService {

    private EhrExtractStatusRepository ehrExtractStatusRepository;

    public Optional<EhrStatus> getEhrStatus(String conversationId) {

        Optional<EhrExtractStatus> extractStatusOptional = ehrExtractStatusRepository.findByConversationId(conversationId);

        if (extractStatusOptional.isEmpty()) {
            return Optional.empty();
        }

        EhrExtractStatus ehrExtractStatus = extractStatusOptional.get();
        List<EhrExtractStatus.EhrReceivedAcknowledgement> receivedAcknowledgements = getAckModel(ehrExtractStatus);
        List<EhrStatus.AttachmentStatus> attachmentStatusList = getAttachmentStatusList(ehrExtractStatus, receivedAcknowledgements);

        return Optional.of(
            EhrStatus.builder()
                .attachmentStatus(attachmentStatusList)
                .migrationLog(receivedAcknowledgements)
                .migrationStatus(evaluateMigrationStatus(ehrExtractStatus, attachmentStatusList))
                .originalRequestDate(ehrExtractStatus.getCreated())
                .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
                .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
                .build());
    }

}
