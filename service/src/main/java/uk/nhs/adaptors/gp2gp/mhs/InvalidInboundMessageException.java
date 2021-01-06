package uk.nhs.adaptors.gp2gp.mhs;

public class InvalidInboundMessageException extends RuntimeException {
    public InvalidInboundMessageException(String errorMessage) {
        super(errorMessage);
    }
}
