package uk.nhs.adaptors.gp2gp.gpc.exception;

public class GpConnectNotFoundException extends RuntimeException {
    public GpConnectNotFoundException(String message) {
        super(message);
    }

    public GpConnectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
