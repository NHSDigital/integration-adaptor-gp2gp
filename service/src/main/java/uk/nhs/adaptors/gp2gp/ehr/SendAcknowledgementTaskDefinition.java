package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.SEND_ACKNOWLEDGEMENT;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

/**
 * Task definition for sending acknowledgment
 */
@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class SendAcknowledgementTaskDefinition extends TaskDefinition {

    public static final String ACK_TYPE_CODE = "AA";
    public static final String NACK_TYPE_CODE = "AE";

    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/recordTarget/patient/id/@extension} of SOAP message payload
     */
    private final String nhsNumber;
    /**
     * ACK code. One of {@code [AA, AE]}
     */
    private final String typeCode;
    /**
     * Value from {@code /Envelope/Header/MessageHeader/MessageData/MessageId} of the SOAP message header
     */
    private final String ehrRequestMessageId;
    /**
     * Code associated with the error. Sent only in case of a negative acknowledgment
     */
    private final String reasonCode;
    /**
     * Description associated with the error. Sent only in case of a negative acknowledgment
     */
    private final String detail;

    @Override
    public TaskType getTaskType() {
        return SEND_ACKNOWLEDGEMENT;
    }

    public boolean isNack() {
        return NACK_TYPE_CODE.equals(typeCode);
    }
}
