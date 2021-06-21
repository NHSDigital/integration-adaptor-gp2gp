package uk.nhs.adaptors.gp2gp.ehr.exception;

public class EhrExtractMessageOutOfOrderException extends RuntimeException {
    public EhrExtractMessageOutOfOrderException(String messageType, String conversationId) {
        super("Received a " + messageType + " message with a Conversation-Id '" + conversationId
            + "' that is out of order in message process");
    }
}
