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
public class EhrStatusService {

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
                .build());
    }

    private List<EhrExtractStatus.EhrReceivedAcknowledgement> getAckModel(EhrExtractStatus ehrExtractStatus) {

        Optional<EhrExtractStatus.AckHistory> ackHistoryOptional = Optional.ofNullable(ehrExtractStatus.getAckHistory());

        return ackHistoryOptional.map(EhrExtractStatus.AckHistory::getAcks).orElse(new ArrayList<>());
    }

    private MigrationStatus evaluateMigrationStatus(EhrExtractStatus record, List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        var ackPendingOptional = Optional.ofNullable(record.getAckPending());
        var ackToRequestorOptional = Optional.ofNullable(record.getAckToRequester());
        var errorOptional = Optional.ofNullable(record.getError());
        var receivedAcknowledgementOptional = Optional.ofNullable(record.getEhrReceivedAcknowledgement());

        if (
            ackPendingOptional
                .map(ackPending -> ackPending.getTypeCode().equals(NACK_TYPE_CODE))
                .orElse(false)

                && errorOptional.isPresent()) {

            return FAILED_NME;
        } else if (
            ackPendingOptional
                .map(ackPending -> ackPending.getTypeCode().equals(ACK_TYPE_CODE))
                .orElse(false)

                && ackToRequestorOptional
                .map(ackToRequester -> ackToRequester.getTypeCode().equals(ACK_TYPE_CODE))
                .orElse(false)

                && errorOptional.isEmpty()

                && receivedAcknowledgementOptional
                .map(acknowledgement ->
                    Optional.ofNullable(acknowledgement.getConversationClosed()).isPresent()
                        && Optional.ofNullable(acknowledgement.getErrors()).isEmpty())
                .orElse(false)) {

            return checkForPlaceholderOrError(attachmentStatusList) ? COMPLETE_WITH_ISSUES : COMPLETE;
        } else if (

            errorOptional.isEmpty()

                && receivedAcknowledgementOptional
                .map(acknowledgement ->
                    Optional.ofNullable(acknowledgement.getErrors()).isPresent())
                .orElse(false)) {

            return FAILED_INCUMBENT;
        }

        return IN_PROGRESS;
    }

    private List<EhrStatus.AttachmentStatus> getAttachmentStatusList(EhrExtractStatus record,
        List<EhrExtractStatus.EhrReceivedAcknowledgement> acknowledgements) {

        List<EhrStatus.AttachmentStatus> attachmentStatusList = new ArrayList<>();

        var accessDocumentOptional = Optional.ofNullable(record.getGpcAccessDocument());

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

    public FileStatus getFileStatus(EhrExtractStatus.GpcDocument document,
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
