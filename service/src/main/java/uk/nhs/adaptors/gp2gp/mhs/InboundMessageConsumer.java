package uk.nhs.adaptors.gp2gp.mhs;

import javax.jms.JMSException;
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
public class InboundMessageConsumer {
    private final InboundMessageHandler inboundMessageHandler;
    private final MDCService mdcService;

//    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}")
    @SneakyThrows
    public void receive(Message message) {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received inbound MHS message {}", messageID);
        try {
            inboundMessageHandler.handle(message);
            message.acknowledge();
            LOGGER.info("Acknowledged inbound MHS message {}", messageID);
        } catch (Exception e) {
            LOGGER.error("An error occurred while handing MHS inbound message {}", messageID, e);
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
