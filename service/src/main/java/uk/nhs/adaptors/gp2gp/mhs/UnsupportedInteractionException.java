package uk.nhs.adaptors.gp2gp.mhs;

public class UnsupportedInteractionException extends RuntimeException {

    public UnsupportedInteractionException(String interactionId) {
        super("Unsupported interaction id " + interactionId);
    }

}
