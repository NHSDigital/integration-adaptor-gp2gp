package uk.nhs.adaptors.gp2gp.common.exception;

public class TaskHandlerException extends RuntimeException {
    public TaskHandlerException(String message) {
        super(message);
    }

    public TaskHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
