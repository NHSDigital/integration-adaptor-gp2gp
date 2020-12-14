package uk.nhs.adaptors.gp2gp.tasks;

import static uk.nhs.adaptors.gp2gp.common.constants.Constants.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.constants.Constants.STRUCTURE_TASK;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDefinitionFactory {
    private final ObjectMapper objectMapper;

    public Optional<TaskDefinition> getTaskDefinition(String taskName, String body) throws JsonProcessingException {
        if (taskName.equalsIgnoreCase(DOCUMENT_TASK)) {
            return Optional.of(objectMapper.readValue(body, GetGpcDocumentTaskDefinition.class));
        }
        if (taskName.equalsIgnoreCase(STRUCTURE_TASK)) {
            return Optional.of(objectMapper.readValue(body, GetGpcStructuredTaskDefinition.class));
        }
        return Optional.empty();
    }
}
