package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.COMPLETE_WITH_ISSUES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusService {

    private EhrExtractStatusRepository ehrExtractStatusRepository;
    public Optional<EhrExtractStatus.EhrStatus> getEhrStatus(String conversationId) {

        Optional<EhrExtractStatus> extractStatusOptional = ehrExtractStatusRepository.findByConversationId(conversationId);

        return extractStatusOptional.map(this::saveMockStatus);
    }

    private EhrExtractStatus.EhrStatus saveMockStatus(EhrExtractStatus ehrExtractStatus) {

        var attachmentStatus = EhrExtractStatus.EhrStatus.AttachmentStatus.builder()
            .documentReferenceId("testRef")
            .isPlaceholder(true)
            .name("test")
            .build();

        EhrExtractStatus.EhrStatus ehrStatus = EhrExtractStatus.EhrStatus.builder()
            .attachmentStatus(List.of(attachmentStatus))
            .migrationStatus(COMPLETE_WITH_ISSUES)
            .acknowledgementModel(new ArrayList<>())
            .originalRequestDate(LocalDateTime.now())
            .build();

        ehrExtractStatus.setEhrStatus(ehrStatus);

        var savedEhrExtractStatus = ehrExtractStatusRepository.save(ehrExtractStatus);

        return savedEhrExtractStatus.getEhrStatus();
    }
}
