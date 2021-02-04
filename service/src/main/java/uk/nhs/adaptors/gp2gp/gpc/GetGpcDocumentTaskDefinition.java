package uk.nhs.adaptors.gp2gp.gpc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

import org.apache.commons.lang3.StringUtils;

@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class GetGpcDocumentTaskDefinition extends TaskDefinition {
    private final String documentId;
    private final String accessDocumentUrl;
    private static final String SLASH = "/";

    @Override
    public TaskType getTaskType() {
        return TaskType.GET_GPC_DOCUMENT;
    }

    public static String extractIdFromUrl(String url) {
        return StringUtils.substring(url, StringUtils.lastIndexOf(url, SLASH) + 1);
    }
}
