package uk.nhs.adaptors.gp2gp.common.task;

import static uk.nhs.adaptors.gp2gp.common.enums.GpcEnums.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.enums.GpcEnums.STRUCTURE_TASK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDefinitionFactory {
    private final ObjectMapper objectMapper;

    public TaskDefinition getTaskDefinition(String taskType, String body) throws JsonProcessingException, TaskHandlerException {
        if (taskType.equalsIgnoreCase(DOCUMENT_TASK.getValue())) {
            return objectMapper.readValue(body, GetGpcDocumentTaskDefinition.class);
        } else if (taskType.equalsIgnoreCase(STRUCTURE_TASK.getValue())) {
            return objectMapper.readValue(body, GetGpcStructuredTaskDefinition.class);
        } else {
            throw new TaskHandlerException("No task definition class for task type '" + taskType + "'");
        }
    }
}
