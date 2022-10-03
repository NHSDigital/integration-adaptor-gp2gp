package uk.nhs.adaptors.gp2gp.ehr.status.service;

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
        List<EhrExtractStatus.EhrReceivedAcknowledgement> receivedAcknowledgements = getAckModel(ehrExtractStatus);
        List<EhrStatus.AttachmentStatus> attachmentStatusList = getAttachmentStatusList(ehrExtractStatus, receivedAcknowledgements);

        return Optional.of(
            EhrStatus.builder()
                .attachmentStatus(attachmentStatusList)
                .receivedAcknowledgements(receivedAcknowledgements)
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
                .map(ackPending -> ackPending.getTypeCode().equals("AE"))
                .orElse(false)

                && ackToRequestorOptional
                .map(ackToRequester -> ackToRequester.getTypeCode().equals("AE"))
                .orElse(false)

                && errorOptional.isPresent()) {

            return FAILED_NME;
        } else if (
            ackPendingOptional
                .map(ackPending -> ackPending.getTypeCode().equals("AA"))
                .orElse(false)

                && ackToRequestorOptional
                .map(ackToRequester -> ackToRequester.getTypeCode().equals("AA"))
                .orElse(false)

                && errorOptional.isEmpty()

                && receivedAcknowledgementOptional
                .map(acknowledgement ->
                    Optional.ofNullable(acknowledgement.getConversationClosed()).isPresent()
                        && Optional.ofNullable(acknowledgement.getErrors()).isEmpty())
                .orElse(false)) {

            return checkForPlaceholderOrError(attachmentStatusList) ? COMPLETE_WITH_ISSUES : COMPLETE;
        } else if (

            // if a NACK or rejected message is received before the continue message these fields won't be populated
//            ackPendingOptional
//                .map(ackPending -> ackPending.getTypeCode().equals("AA"))
//                .orElse(false)
//
//                && ackToRequestorOptional
//                .map(ackToRequester -> ackToRequester.getTypeCode().equals("AA"))
//                .orElse(false)
//
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

    public FileStatus getFileStatus(EhrExtractStatus.GpcDocument document, List<EhrExtractStatus.EhrReceivedAcknowledgement> acknowledgements) {

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

        if (objectNameOptional.isPresent() && objectNameOptional.get().contains("AbsentAttachment")) {
            return PLACEHOLDER;
        }

        if (fileNameOptional.isPresent() && fileNameOptional.get().contains("AbsentAttachment")) {
            return PLACEHOLDER;
        }

        return ORIGINAL_FILE;
    }

    private Boolean checkForPlaceholderOrError(List<EhrStatus.AttachmentStatus> attachmentStatusList) {

        return attachmentStatusList.stream()
            .anyMatch(attachmentList ->
                attachmentList.getFileStatus().equals(PLACEHOLDER) ||
                attachmentList.getFileStatus().equals(ERROR));
    }
}
