package uk.nhs.adaptors.gp2gp.common.task;

import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskConsumer {

    private final TaskHandler taskHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.taskQueueName}")
    @SneakyThrows
    public void receive(Message message) {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received message from taskQueue {}", messageID);
        try {
            if (taskHandler.handle(message)) {
                message.acknowledge();
                LOGGER.info("Acknowledged message {}", messageID);
            } else {
                LOGGER.info("Left message {} on the queue", messageID);
            }

        } catch (Exception e) {
            LOGGER.error("Error while processing task queue message {}", messageID, e);
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
