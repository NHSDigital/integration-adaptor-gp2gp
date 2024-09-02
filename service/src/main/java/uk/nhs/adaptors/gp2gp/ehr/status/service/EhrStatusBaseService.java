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
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.FileStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusBaseService {

    private static final String ACK_TYPE_CODE = "AA";
    private static final String NACK_TYPE_CODE = "AE";

    protected List<EhrExtractStatus.EhrReceivedAcknowledgement> getAckModel(EhrExtractStatus ehrExtractStatus) {

        Optional<EhrExtractStatus.AckHistory> ackHistoryOptional = Optional.ofNullable(ehrExtractStatus.getAckHistory());

        return ackHistoryOptional.map(EhrExtractStatus.AckHistory::getAcks).orElse(new ArrayList<>());
    }

    protected MigrationStatus evaluateMigrationStatus(EhrExtractStatus ehrExtractStatus,
                                                      List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        var ackPendingOptional = Optional.ofNullable(ehrExtractStatus.getAckPending());
        var ackToRequestorOptional = Optional.ofNullable(ehrExtractStatus.getAckToRequester());
        var errorOptional = Optional.ofNullable(ehrExtractStatus.getError());
        var receivedAcknowledgementOptional = Optional.ofNullable(ehrExtractStatus.getEhrReceivedAcknowledgement());

        if (ackPendingOptional.map(ackPending -> NACK_TYPE_CODE.equals(ackPending.getTypeCode())).orElse(false)
            && errorOptional.isPresent()) {
            return FAILED_NME;
        } else if (ackPendingOptional.map(ackPending -> ACK_TYPE_CODE.equals(ackPending.getTypeCode())).orElse(false)
                    && ackToRequestorOptional.map(ackToRequester -> ACK_TYPE_CODE.equals(ackToRequester.getTypeCode())).orElse(false)
                    && errorOptional.isEmpty()
                    && receivedAcknowledgementOptional
                    .map(acknowledgement ->
                        Optional.ofNullable(acknowledgement.getConversationClosed()).isPresent()
                            && Optional.ofNullable(acknowledgement.getErrors()).isEmpty())
                    .orElse(false)) {

            return checkForPlaceholderOrError(attachmentStatusList) ? COMPLETE_WITH_ISSUES : COMPLETE;
        } else if (errorOptional.isEmpty()
                   && receivedAcknowledgementOptional.map(acknowledgement -> Optional.ofNullable(acknowledgement.getErrors()).isPresent())
                        .orElse(false)) {

            return FAILED_INCUMBENT;
        }

        return IN_PROGRESS;
    }

    protected List<EhrStatus.AttachmentStatus> getAttachmentStatusList(EhrExtractStatus ehrExtractStatus,
                                                                       List<EhrExtractStatus.EhrReceivedAcknowledgement> acknowledgements) {

        List<EhrStatus.AttachmentStatus> attachmentStatusList = new ArrayList<>();

        var accessDocumentOptional = Optional.ofNullable(ehrExtractStatus.getGpcAccessDocument());

        accessDocumentOptional.ifPresent(accessDocument ->
            accessDocument.getDocuments().forEach(gpcDocument ->
                attachmentStatusList.add(
                    EhrStatus.AttachmentStatus.builder()
                        .identifier(gpcDocument.getIdentifier())
                        .fileName(gpcDocument.getFileName())
                        .fileStatus(getFileStatus(gpcDocument, acknowledgements))
                        .originalDescription(gpcDocument.getOriginalDescription())
                        .build())
            )
        );

        return attachmentStatusList.stream()
            .filter(attachmentStatus -> attachmentStatus.getFileStatus() != SKELETON_MESSAGE)
            .collect(Collectors.toList());
    }

    protected FileStatus getFileStatus(EhrExtractStatus.GpcDocument document,
        List<EhrExtractStatus.EhrReceivedAcknowledgement> acknowledgements) {

        Optional<String> fileNameOptional = Optional.ofNullable(document.getFileName());
        Optional<String> objectNameOptional = Optional.ofNullable(document.getObjectName());
        Optional<EhrExtractStatus.GpcAccessDocument.SentToMhs> sentToMhsOptional = Optional.ofNullable(document.getSentToMhs());

        if (document.isSkeleton()) {
            return SKELETON_MESSAGE;
        }

        if (sentToMhsOptional.isPresent()) {
            List<String> messageIds = sentToMhsOptional.get().getMessageId();
            boolean documentHasNack = acknowledgements.stream()
                .filter(ack -> ack.getErrors() != null)
                .anyMatch(ack -> messageIds.contains(ack.getMessageRef()));

            if (documentHasNack) {
                return ERROR;
            }
        }

        boolean isPlaceholder = hasAbsentAttachmentPrefix(objectNameOptional) || hasAbsentAttachmentPrefix(fileNameOptional);

        if (isPlaceholder) {
            return PLACEHOLDER;
        }

        return ORIGINAL_FILE;
    }

    private boolean checkForPlaceholderOrError(List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        return attachmentStatusList.stream()
            .anyMatch(attachmentList ->
                attachmentList.getFileStatus().equals(PLACEHOLDER)
                    || attachmentList.getFileStatus().equals(ERROR));
    }

    private boolean hasAbsentAttachmentPrefix(Optional<String> filenameOptional) {
        return filenameOptional.isPresent() && filenameOptional.get().startsWith("AbsentAttachment");
    }
}
