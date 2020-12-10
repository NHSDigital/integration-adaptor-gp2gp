package uk.nhs.adaptors.gp2gp.tasks;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.utils.JmsReader;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskQueueConsumer {
    private static final String MESSAGE_CLASS = "class uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskDefinition";

    private final ObjectMapper objectMapper;
    private final TaskService taskService;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    public void receive(Message message) throws IOException, JMSException, ClassNotFoundException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received message from taskQueue {}", messageID);
        var taskName = message.getStringProperty(MESSAGE_CLASS);
        Class<? extends TaskDefinition> selectedClass = (Class<? extends TaskDefinition>) Class.forName(taskName);

        try {
            String body = JmsReader.readMessage(message);
            TaskDefinition taskDefinition = objectMapper.readValue(body, selectedClass);
            LOGGER.info("Task name {}, taskId {}, extractID {}, conversationId {}",
                taskName, taskDefinition.getTaskId(), taskDefinition.getRequestId(), taskDefinition.getConversationId());
            taskService.handleRequest(taskDefinition);
            message.acknowledge();
            LOGGER.info("Acknowledged message {}", messageID);
        } catch (Exception e) {
            LOGGER.error("Error while processing MHS inbound queue message {}", messageID, e);
            throw e; //message will be sent to DLQ after few unsuccessful redeliveries
        }
    }
}
