package uk.nhs.adaptors.gp2gp.common.task;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

import javax.jms.Message;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskConsumer {

    private final TaskHandler taskHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.taskQueueName}", concurrency = "${gp2gp.amqp.taskQueueConsumerConcurrency}")
    @SneakyThrows
    public void receive(Message message) {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received taskQueue message_id: {}", messageID);
        try {
            if (taskHandler.handle(message)) {
                message.acknowledge();
                LOGGER.info("Acknowledged taskQueue message_id: {}", messageID);
            } else {
                LOGGER.info("Leaving taskQueue message_id: {} on the queue", messageID);
            }

        } catch (Exception e) {
            LOGGER.error("Error while processing taskQueue message_id: {}", messageID, e);
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
