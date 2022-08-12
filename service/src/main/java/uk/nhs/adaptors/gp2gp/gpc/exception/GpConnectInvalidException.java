package uk.nhs.adaptors.gp2gp.gpc.exception;

public class GpConnectInvalidException extends RuntimeException{
    public GpConnectInvalidException(String message) {
        super(message);
    }

    public GpConnectInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
