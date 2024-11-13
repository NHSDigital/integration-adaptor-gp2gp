package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_STRUCTURED;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

/**
 * Task definition for downloading Structured Record from GCP
 */
@Jacksonized
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetGpcStructuredTaskDefinition extends TaskDefinition {
    /**
     * Value from {@code /RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/recordTarget/patient/id/@extension} of SOAP message payload
     */
    private final String nhsNumber;

    @Override
    public TaskType getTaskType() {
        return GET_GPC_STRUCTURED;
    }

    public static GetGpcStructuredTaskDefinition getGetGpcStructuredTaskDefinition(RandomIdGeneratorService randomIdGeneratorService,
                                                                                   EhrExtractStatus ehrExtractStatus) {
        var getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
            .taskId(randomIdGeneratorService.createNewId())
            .conversationId(ehrExtractStatus.getConversationId())
            .requestId(ehrExtractStatus.getEhrRequest().getRequestId())
            .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
            .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
            .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
            .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
            .build();
        return getGpcStructuredTaskDefinition;
    }
}
