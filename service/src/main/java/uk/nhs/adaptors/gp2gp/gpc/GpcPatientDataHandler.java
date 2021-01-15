package uk.nhs.adaptors.gp2gp.gpc;

import static java.time.Instant.now;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcPatientDataHandler {
    private final EhrExtractStatusRepository ehrExtractStatusRepository;

    private EhrExtractStatus getEhrExtractStatus(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Optional<EhrExtractStatus> ehrStatus =
            ehrExtractStatusRepository.findByConversationId(structuredTaskDefinition.getConversationId());
        if (ehrStatus.isPresent()) {
            return ehrStatus.get();
        } else {
            throw new RuntimeException("No EHR Extract Status for ConversationId: " + structuredTaskDefinition.getConversationId());
        }
    }

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        var ehrExtractStatus = getEhrExtractStatus(structuredTaskDefinition);
        var accessStructured = new EhrExtractStatus.GpcAccessStructured(ehrExtractStatus.getConversationId()
            + GPC_STRUCTURED_FILE_EXTENSION, now(),
            structuredTaskDefinition.getTaskId());
        ehrExtractStatus.setGpcAccessStructured(accessStructured);
        ehrExtractStatus.setUpdatedAt(now());
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }
}
