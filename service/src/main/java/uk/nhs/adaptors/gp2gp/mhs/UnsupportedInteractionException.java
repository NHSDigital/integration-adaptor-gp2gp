package uk.nhs.adaptors.gp2gp.mhs;

public class UnsupportedInteractionException extends RuntimeException {

    public UnsupportedInteractionException(String interactionId) {
        super("Unsupported interaction id " + interactionId);
    }

    public static class InvalidInboundMessageException extends RuntimeException {
        public InvalidInboundMessageException(String errorMessage) {
            super(errorMessage);
        }
    }
}
