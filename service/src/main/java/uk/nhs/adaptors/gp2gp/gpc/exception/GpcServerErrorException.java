package uk.nhs.adaptors.gp2gp.gpc.exception;

public class GpcServerErrorException extends RuntimeException {
    public GpcServerErrorException(String message) {
        super(message);
    }
}
