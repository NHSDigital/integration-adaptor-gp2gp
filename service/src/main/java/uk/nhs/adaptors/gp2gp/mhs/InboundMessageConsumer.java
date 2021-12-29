package uk.nhs.adaptors.gp2gp.mhs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

import javax.jms.JMSException;
import javax.jms.Message;

import static uk.nhs.adaptors.gp2gp.common.utils.Jms.getJmsMessageTimestamp;

import java.time.OffsetDateTime;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {
    private final InboundMessageHandler inboundMessageHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}", concurrency = "${gp2gp.amqp.inboundQueueConsumerConcurrency}")
    public void receive(Message message) throws JMSException {
        LOGGER.info("TIMEMEASURE >> Message got from the queue on timestamp: [{}]", OffsetDateTime.now());
        var messageID = message.getJMSMessageID();
        var messageTimestamp = getJmsMessageTimestamp(message);
        LOGGER.info("Received inbound MHS message_id: {} timestamp: {}", messageID, messageTimestamp);
        try {
            if (inboundMessageHandler.handle(message)) {
                message.acknowledge();
                LOGGER.info("Acknowledged inbound MHS message_id: {}", messageID);
            } else {
                LOGGER.info("Leaving inbound MHS message_id: {} on the queue", messageID);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred while handing inbound MHS message_id: {}", messageID, e);
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
