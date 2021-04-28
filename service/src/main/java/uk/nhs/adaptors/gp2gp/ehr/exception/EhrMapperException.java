package uk.nhs.adaptors.gp2gp.ehr.exception;

public class EhrMapperException extends RuntimeException {
    public EhrMapperException(String message) {
        super(message);
    }

    public EhrMapperException(String message, Exception exception) {
        super(message, exception);
    }
}
