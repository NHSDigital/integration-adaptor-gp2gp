package uk.nhs.adaptors.gp2gp.gpc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;
import uk.nhs.adaptors.gp2gp.ehr.DocumentTaskDefinition;

import static uk.nhs.adaptors.gp2gp.common.task.TaskType.GET_GPC_DOCUMENT;

@Jacksonized
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
public class GetGpcDocumentTaskDefinition extends DocumentTaskDefinition {
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
