package uk.nhs.adaptors.gp2gp.mhs.exception;

public class NonExistingInteractionIdException extends RuntimeException {
    public NonExistingInteractionIdException(String messageType, String conversationId) {
        super("Received a " + messageType + " message that is not recognised with conversation_id: " + conversationId);
    }
}
