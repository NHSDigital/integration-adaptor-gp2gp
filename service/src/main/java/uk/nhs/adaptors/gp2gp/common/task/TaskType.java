package uk.nhs.adaptors.gp2gp.common.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.SendAcknowledgementTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

public enum TaskType {
    GET_GPC_DOCUMENT(GetGpcDocumentTaskDefinition.class),
    GET_GPC_STRUCTURED(GetGpcStructuredTaskDefinition.class),
    SEND_EHR_EXTRACT_CORE(SendEhrExtractCoreTaskDefinition.class),
    SEND_EHR_CONTINUE(SendDocumentTaskDefinition.class),
    SEND_ACKNOWLEDGEMENT(SendAcknowledgementTaskDefinition.class);

    @Getter
    private final Class<? extends TaskDefinition> classOfTaskDefinition;
    @Getter
    private final String taskName;

    TaskType(Class<? extends TaskDefinition> classOfTaskDefinition) {
        this.classOfTaskDefinition = classOfTaskDefinition;
        this.taskName = classOfTaskDefinition.getName();
    }
}
