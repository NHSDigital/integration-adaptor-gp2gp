package uk.nhs.adaptors.gp2gp.mhs;

public class InvalidOutboundMessageException extends RuntimeException {
    public InvalidOutboundMessageException(String message) {
        super(message);
    }
}
