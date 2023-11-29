package uk.nhs.adaptors.gp2gp.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Component
public class ProcessDetectionService {

    private final EhrExtractStatusRepository ehrExtractStatusRepository;

    private static final String ACK_TYPE_CODE = "AA";

    @Autowired
    public ProcessDetectionService(EhrExtractStatusRepository ehrExtractStatusRepository) {
        this.ehrExtractStatusRepository = ehrExtractStatusRepository;
    }

    public boolean awaitingContinue(String conversationId) {
        var dbExtractOptional = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (dbExtractOptional.isEmpty()) {
            return false;
        }
        var dbExtract = dbExtractOptional.orElseThrow();
        var ehrCorePendingOptional = Optional.ofNullable(dbExtract.getEhrExtractCorePending());
        var ackPending = Optional.ofNullable(dbExtract.getAckPending());

        return ehrCorePendingOptional.isPresent() && ackPending.isEmpty();
    }

    public boolean awaitingAck(String conversationId) {
        var dbExtractOptional = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (dbExtractOptional.isEmpty()) {
            return false;
        }
        var dbExtract = dbExtractOptional.orElseThrow();
        var ackPending = Optional.ofNullable(dbExtract.getAckPending());

        return ackPending.isPresent();
    }

    public boolean transferComplete(String conversationId) {
        var finalDbExtractOptional = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (finalDbExtractOptional.isEmpty()) {
            return false;
        }
        var finalDbExtract = finalDbExtractOptional.orElseThrow();
        var ackPendingOptional = Optional.ofNullable(finalDbExtract.getAckPending());
        var ackToRequestorOptional = Optional.ofNullable(finalDbExtract.getAckToRequester());
        var errorOptional = Optional.ofNullable(finalDbExtract.getError());
        var receivedAcknowledgementOptional = Optional.ofNullable(finalDbExtract.getEhrReceivedAcknowledgement());

        return ackPendingOptional
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
            .orElse(false);
    }

    public boolean processFailed(String conversationId) {
        var finalDbExtractOptional = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (finalDbExtractOptional.isEmpty()) {
            return false;
        }
        var finalDbExtract = finalDbExtractOptional.orElseThrow();
        var extractError = Optional.ofNullable(finalDbExtract.getError());
        var receivedEhrAck = Optional.ofNullable(finalDbExtract.getEhrReceivedAcknowledgement());
        boolean hasIncumbentNack = receivedEhrAck
            .map(ehrAck -> ehrAck.getErrors().size() > 0)
            .orElse(false);

        return extractError.isPresent() || hasIncumbentNack;
    }

    public boolean nackReceived(String conversationId) {
        var dbExtractOptional = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (dbExtractOptional.isEmpty()) {
            return false;
        }

        var dbExtract = dbExtractOptional.orElseThrow();
        var ackHistory = Optional.ofNullable(dbExtract.getAckHistory());
        if (ackHistory.isEmpty()) {
            return false;
        }

        var receivedAcks = ackHistory
            .map(EhrExtractStatus.AckHistory::getAcks)
            .orElseThrow();

        var negativeAck = receivedAcks.stream()
            .filter(ack -> ack.getErrors().size() > 0)
            .findFirst();

        return negativeAck.isPresent();
    }
}
