package uk.nhs.adaptors.gp2gp.common.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GpcFindDocumentsTaskDefinition;

@Getter
@RequiredArgsConstructor
public enum TaskType {
    GET_GPC_DOCUMENT(GetGpcDocumentTaskDefinition.class),
    GET_GPC_STRUCTURED(GetGpcStructuredTaskDefinition.class),
    GPC_FIND_DOCUMENTS(GpcFindDocumentsTaskDefinition.class);

    private final Class<? extends TaskDefinition> classOfTaskDefinition;

    public String getTaskTypeHeaderValue() {
        return classOfTaskDefinition.getName();
    }
}
