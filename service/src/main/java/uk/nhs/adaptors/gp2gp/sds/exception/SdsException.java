package uk.nhs.adaptors.gp2gp.sds.exception;

public class SdsException extends RuntimeException {
    public SdsException(String message) {
        super(message);
    }

    public SdsException(String message, Throwable cause) {
        super(message, cause);
    }
}
