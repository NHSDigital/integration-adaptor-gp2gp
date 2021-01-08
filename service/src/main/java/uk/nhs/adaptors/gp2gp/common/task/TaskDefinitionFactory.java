package uk.nhs.adaptors.gp2gp.common.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDefinitionFactory {
    private final ObjectMapper objectMapper;

    public TaskDefinition getTaskDefinition(String taskType, String body)  {
        return Arrays.stream(TaskType.values())
            .filter(type -> type.getTaskTypeHeaderValue().equals(taskType))
            .map(type -> unmarshallTask(type, body))
            .findFirst()
            .orElseThrow(() -> new TaskHandlerException("No task definition class for task type '" + taskType + "'"));
    }

    private TaskDefinition unmarshallTask(TaskType taskType, String body) {
        try {
            return objectMapper.readValue(body, taskType.getClassOfTaskDefinition());
        } catch (JsonProcessingException e) {
            throw new TaskHandlerException("Unable to unmarshall task definition", e);
        }
    }
}
