package uk.nhs.adaptors.gp2gp.ehr.status.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

import static uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus.*;
import static uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus.*;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusService {

    private EhrExtractStatusRepository ehrExtractStatusRepository;

    public Optional<EhrStatus> getEhrStatus(String conversationId) {

        Optional<EhrExtractStatus> extractStatusOptional = ehrExtractStatusRepository.findByConversationId(conversationId);

        if (extractStatusOptional.isEmpty()) {
            return Optional.empty();
        }

        EhrExtractStatus ehrExtractStatus = extractStatusOptional.get();
        List<EhrStatus.AttachmentStatus> attachmentStatusList = getAttachmentStatusList(ehrExtractStatus);

        return Optional.of(
            EhrStatus.builder()
                .attachmentStatus(attachmentStatusList)
                .acknowledgementModel(getAckModel(ehrExtractStatus))
                .migrationStatus(evaluateMigrationStatus(ehrExtractStatus, attachmentStatusList))
                .originalRequestDate(ehrExtractStatus.getCreated())
                .build());
    }

    private List<EhrExtractStatus.EhrReceivedAcknowledgement> getAckModel(EhrExtractStatus ehrExtractStatus) {

        Optional<EhrExtractStatus.AckHistory> ackHistoryOptional = Optional.ofNullable(ehrExtractStatus.getAckHistory());

        return ackHistoryOptional.map(EhrExtractStatus.AckHistory::getAcks).orElse(new ArrayList<>());
    }

    /**
     * TODO Need to implement COMPLETE_WITH_ISSUES circumstances once ACK/NACK History is accessible
     */
    private MigrationStatus evaluateMigrationStatus(EhrExtractStatus record, List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        if (record.getAckPending().getTypeCode().equals("AE")
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

            return checkListContainsPlaceholder(attachmentStatusList)
                ? COMPLETE_WITH_ISSUES : COMPLETE;
        }

        return null;
    }

    private List<EhrStatus.AttachmentStatus> getAttachmentStatusList(EhrExtractStatus record) {

        List<EhrStatus.AttachmentStatus> attachmentStatusList = new ArrayList<>();

        var accessDocumentOptional = Optional.ofNullable(record.getGpcAccessDocument());

        accessDocumentOptional.ifPresent(accessDocument ->
            accessDocument.getDocuments().forEach(gpcDocument ->
                attachmentStatusList.add(
                    EhrStatus.AttachmentStatus.builder()
                        .url(gpcDocument.getAccessDocumentUrl())
                        .title(gpcDocument.getTitle())
                        .fileStatus(getFileStatus(gpcDocument)) // TODO: check if there are NACK messages for the document
                        .documentReferenceId(gpcDocument.getDocumentReferenceId())
                        .build())
            )
        );

        return attachmentStatusList;
    }

    private FileStatus getFileStatus(EhrExtractStatus.GpcDocument document) {

        Optional<String> docRefOptional = Optional.ofNullable(document.getDocumentReferenceId());

        if (docRefOptional.isEmpty()) {
            return SKELETON_MESSAGE;
        }

        Optional<String> fileNameOptional = Optional.ofNullable(document.getFileName());

        return fileNameOptional
            .map(filename -> filename.contains("AbsentAttachment") ? PLACEHOLDER : ORIGINAL_FILE)
            .orElse(ORIGINAL_FILE);
    }

    private Boolean checkListContainsPlaceholder(List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        return attachmentStatusList.stream().anyMatch(
            attachmentList -> attachmentList.getFileStatus().equals(PLACEHOLDER));
    }
}
