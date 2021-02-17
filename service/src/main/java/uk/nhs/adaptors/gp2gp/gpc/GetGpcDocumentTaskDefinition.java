package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_DOCUMENT;

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;

@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class GetGpcDocumentTaskDefinition extends TaskDefinition {
    private final String documentId;
    private final String accessDocumentUrl;
    private static final String PATH_DELIMITER = "/";

    @Override
    public TaskType getTaskType() {
        return GET_GPC_DOCUMENT;
    }

    public static String extractIdFromUrl(String url) {
        return StringUtils.substring(url, StringUtils.lastIndexOf(url, PATH_DELIMITER) + 1);
    }
}
