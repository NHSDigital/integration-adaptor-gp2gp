package uk.nhs.adaptors.gp2gp.common.exception;

public class RetryLimitReachedException extends RuntimeException {

    public RetryLimitReachedException(String message, Throwable cause) {
        super(message, cause);
    }
}
