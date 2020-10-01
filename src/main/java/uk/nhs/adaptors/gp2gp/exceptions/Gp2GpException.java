package uk.nhs.adaptors.gp2gp.exceptions;

public class Gp2GpException extends RuntimeException {
    public Gp2GpException() {
    }

    public Gp2GpException(String message) {
        super(message);
    }

    public Gp2GpException(String message, Throwable cause) {
        super(message, cause);
    }

    public Gp2GpException(Throwable cause) {
        super(cause);
    }

    public Gp2GpException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
