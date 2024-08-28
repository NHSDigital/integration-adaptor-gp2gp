package uk.nhs.adaptors.gp2gp.mhs.exception;

public class UnrecognisedInteractionIdException extends RuntimeException {

    private static final String EXCEPTION_MESSAGE = "Received an unrecognized %s message with conversation_id: %s";

    public UnrecognisedInteractionIdException(String messageType, String conversationId) {
        super(EXCEPTION_MESSAGE.formatted(messageType, conversationId));
    }
}
