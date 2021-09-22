package uk.nhs.adaptors.gp2gp.common.task;

import static uk.nhs.adaptors.gp2gp.common.task.TaskHandler.TASK_TYPE_HEADER_NAME;

import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDispatcher {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gp2gp.amqp.taskQueueName}")
    private String taskQueueName;

    public void createTask(TaskDefinition taskDefinition) {
        try {
            String messagePayload = objectMapper.writeValueAsString(taskDefinition);
            sendMessage(messagePayload, taskDefinition.getTaskType().getTaskName());
            LOGGER.info("Created new {} task with id {}",
                taskDefinition.getTaskType().getTaskName(),
                taskDefinition.getTaskId());
        } catch (JsonProcessingException e) {
            throw new TaskHandlerException("Unable to serialise task definition to JSON", e);
        }
    }

    private void sendMessage(String payload, String taskType) {
        jmsTemplate.send(taskQueueName, session -> {
            TextMessage textMessage = session.createTextMessage(payload);
            textMessage.setStringProperty(TASK_TYPE_HEADER_NAME, taskType);
            return textMessage;
        });
    }

}
