package uk.nhs.adaptors.gp2gp.mhs.exception;

public class MessageOutOfOrderException extends RuntimeException {
    public MessageOutOfOrderException(String messageType, String conversationId) {
        super("Received a " + messageType + " message that is out of order in message process with a Conversation-Id: '" + conversationId
                + "'");
    }
}
