package uk.nhs.adaptors.gp2gp.common.task;

import static uk.nhs.adaptors.gp2gp.common.enums.TaskEnums.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.enums.TaskEnums.STRUCTURE_TASK;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDefinitionFactory {
    private final ObjectMapper objectMapper;

    public Optional<TaskDefinition> getTaskDefinition(String taskName, String body) throws JsonProcessingException {
        if (taskName.equalsIgnoreCase(DOCUMENT_TASK.getValue())) {
            return Optional.of(objectMapper.readValue(body, GetGpcDocumentTaskDefinition.class));
        } else if (taskName.equalsIgnoreCase(STRUCTURE_TASK.getValue())) {
            return Optional.of(objectMapper.readValue(body, GetGpcStructuredTaskDefinition.class));
        }
        return Optional.empty();
    }
}
