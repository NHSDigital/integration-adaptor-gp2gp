package uk.nhs.adaptors.gp2gp.common.task;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all types of tasks
 */
@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class TaskDefinition {
    /**
     * Random unique identifier of the task. UUIDv4 format
     */
    private final String taskId;
    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/id/@root} of SOAP message payload
     */
    private final String requestId;
    /**
     * Value from {@code /Envelope/Header/MessageHeader/ConversationId} of SOAP message header
     */
    private final String conversationId;
    /**
     * Value from {@code /RCMR_IN010000UK05/communicationFunctionRcv/device/id/@extension} of SOAP message payload
     */
    private final String toAsid;
    /**
     * Value from {@code /RCMR_IN010000UK05/communicationFunctionSnd/device/id/@extension} of SOAP message payload
     */
    private final String fromAsid;
    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/author/AgentOrgSDS/
       agentOrganizationSDS/id/@extension} of SOAP message payload
     */
    @SuppressWarnings("")
    private final String fromOdsCode;
    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/destination/AgentOrgSDS/
       agentOrganizationSDS/id/@extension} of SOAP message payload
     */
    private final String toOdsCode;

    public abstract TaskType getTaskType();
}
