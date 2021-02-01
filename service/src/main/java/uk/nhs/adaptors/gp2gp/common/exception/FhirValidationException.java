package uk.nhs.adaptors.gp2gp.common.exception;

public class FhirValidationException extends RuntimeException {
    public FhirValidationException(String message) {
        super(message);
    }
}
