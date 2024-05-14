package uk.nhs.adaptors.gp2gp.gpc.exception;

import lombok.Getter;
import org.hl7.fhir.dstu3.model.OperationOutcome;

@Getter
public class GpConnectException extends RuntimeException {

    private OperationOutcome operationOutcome;

    public GpConnectException(String message) {
        super(message);
    }

    public GpConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public GpConnectException(String message, OperationOutcome operationOutcome) {
        super(message);
        this.operationOutcome = operationOutcome.copy();
    }
}
