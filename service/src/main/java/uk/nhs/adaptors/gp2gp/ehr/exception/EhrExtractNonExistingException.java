package uk.nhs.adaptors.gp2gp.ehr.exception;

public class EhrExtractNonExistingException extends RuntimeException {
    public EhrExtractNonExistingException(String messageType, String conversationId) {
        super("Received a " + messageType + " message with a Conversation-Id '" + conversationId
            + "' that is not recognised");
    }
}
