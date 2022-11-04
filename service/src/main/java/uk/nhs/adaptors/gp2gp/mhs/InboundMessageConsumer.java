package uk.nhs.adaptors.gp2gp.mhs;

import static uk.nhs.adaptors.gp2gp.common.utils.Jms.getJmsMessageTimestamp;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageConsumer {
    private final InboundMessageHandler inboundMessageHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.inboundQueueName}", concurrency = "${gp2gp.amqp.inboundQueueConsumerConcurrency}",
        containerFactory = "transactedJmsListenerContainerFactory")
    public void receive(Message message, Session session) throws JMSException, DataAccessResourceFailureException {
        var messageID = message.getJMSMessageID();
        var messageTimestamp = getJmsMessageTimestamp(message);
        LOGGER.info("Received inbound MHS message_id: {} timestamp: {}", messageID, messageTimestamp);

        try {
            if (inboundMessageHandler.handle(message)) {
                message.acknowledge();
                LOGGER.info("Acknowledged inbound MHS message_id: {}", messageID);
            } else {
                LOGGER.info("Unable to handle message_id: {}", messageID);
                session.rollback();
            }
        } catch (DataAccessResourceFailureException e) {
            LOGGER.trace("Caught Data Access Resource Failure Exception and re-throwing it for the error handler");
            throw e;
        } catch (Exception e) {
            LOGGER.error("An error occurred while handing inbound MHS message_id: {}", messageID, e);
            session.rollback();
        } finally {
            mdcService.resetAllMdcKeys();
        }
    }
}
