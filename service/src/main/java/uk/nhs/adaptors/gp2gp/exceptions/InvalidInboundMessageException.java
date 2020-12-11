package uk.nhs.adaptors.gp2gp.exceptions;

public class InvalidInboundMessageException extends RuntimeException {
    public InvalidInboundMessageException(String errorMessage) {
        super(errorMessage);
    }
}
