package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ERROR;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.ORIGINAL_FILE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.PLACEHOLDER;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.SKELETON_MESSAGE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE_WITH_ISSUES;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_INCUMBENT;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.FAILED_NME;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.IN_PROGRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;


@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusService extends EhrStatusBaseService {

    private static final String ACK_TYPE_CODE = "AA";
    private static final String NACK_TYPE_CODE = "AE";

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
