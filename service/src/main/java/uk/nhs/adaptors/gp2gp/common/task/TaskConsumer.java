package uk.nhs.adaptors.gp2gp.common.task;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.utils.ConversationIdService;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskConsumer {

    private final TaskHandler taskHandler;
    private final TaskIdService taskIdService;
    private final ConversationIdService conversationIdService;

    @JmsListener(destination = "${gp2gp.amqp.taskQueueName}")
    public void receive(Message message) throws JsonProcessingException, JMSException, TaskHandlerException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received message from taskQueue {}", messageID);
        try {
            taskHandler.handle(message);
            message.acknowledge();
            LOGGER.info("Acknowledged message {}", messageID);
        } catch (IOException e) {
            LOGGER.error("Error while processing task queue message {}", messageID, e);
            throw e;
        } finally {
            conversationIdService.resetConversationId();
            taskIdService.resetTaskId();
        }
    }
}
