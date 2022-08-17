package uk.nhs.adaptors.gp2gp.ehr.status.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.*;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.*;

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
            .fileStatus(PLACEHOLDER)
            .name("test")
            .build();

        EhrExtractStatus.EhrStatus ehrStatus = EhrExtractStatus.EhrStatus.builder()
            .attachmentStatus(List.of(attachmentStatus))
            .migrationStatus(evaluateMigrationStatus(ehrExtractStatus))
            .acknowledgementModel(new ArrayList<>())
            .originalRequestDate(LocalDateTime.now())
            .build();

        ehrExtractStatus.setEhrStatus(ehrStatus);

        return ehrExtractStatusRepository.save(ehrExtractStatus).getEhrStatus();
    }

   /** TODO Need to implement COMPLETE_WITH_ISSUES circumstances once ACK/NACK History is accessible */
    private MigrationStatus evaluateMigrationStatus(EhrExtractStatus record) {

        if(record.getAckPending().getTypeCode().equals("AE")
                && record.getAckToRequester().getTypeCode().equals("AE")
                    && !Objects.isNull(record.getError())) {
            
            return FAILED_NME;

        } else if (record.getAckPending().getTypeCode().equals("AA")
                    && record.getAckToRequester().getTypeCode().equals("AA")
                        && Objects.isNull(record.getError())
                            && !Objects.isNull(record.getEhrReceivedAcknowledgement().getErrors())) {

            return FAILED_INCUMBENT;
            
        } else if (record.getAckPending().getTypeCode().equals("AA")
                    && record.getAckToRequester().getTypeCode().equals("AA")
                        && Objects.isNull(record.getError())
                            && !Objects.isNull(record.getEhrReceivedAcknowledgement().getConversationClosed())
                                && Objects.isNull(record.getEhrReceivedAcknowledgement().getErrors())) {

            return checkListContainsPlaceholder(attachmentStatusListBuilder(record))
                    ? COMPLETE_WITH_ISSUES : COMPLETE;
        }

        return null;
    }

    /** TODO Find a way to properly pull out the Original Filename and Reference ID (current implementation not appropriate) */
    private List<EhrExtractStatus.EhrStatus.AttachmentStatus> attachmentStatusListBuilder(EhrExtractStatus record) {

        List<EhrExtractStatus.EhrStatus.AttachmentStatus> attachmentStatusList = new ArrayList<>();

        record.getGpcAccessDocument().getDocuments()
                .forEach(
                gpcDocument ->
                attachmentStatusList.add(new EhrExtractStatus.EhrStatus.AttachmentStatus(
                        gpcDocument.getFileName(),
                        checkIfPlaceholder(gpcDocument),
                        gpcDocument.getDocumentId()))
                );

        return attachmentStatusList;
    }

    private FileStatus checkIfPlaceholder(EhrExtractStatus.GpcDocument document) {

       return document.getFileName().equals("AbsentAttachment") ? PLACEHOLDER : ORIGINAL_FILE;
    }

    private Boolean checkListContainsPlaceholder(List<EhrExtractStatus.EhrStatus.AttachmentStatus> attachmentStatusList) {

        return attachmentStatusList.stream().anyMatch(
                attachmentList -> attachmentList.getFileStatus().equals(PLACEHOLDER));
    }
}
