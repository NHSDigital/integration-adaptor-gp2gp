package uk.nhs.adaptors.gp2gp.mhs.exception;

public class UnsupportedInteractionException extends RuntimeException {
    public UnsupportedInteractionException(String interactionId) {
        super("Unsupported interaction id " + interactionId);
    }
}
