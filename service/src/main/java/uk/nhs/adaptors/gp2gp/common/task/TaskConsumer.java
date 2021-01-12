package uk.nhs.adaptors.gp2gp.common.task;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import org.xml.sax.SAXException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskConsumer {

    private final TaskHandler taskHandler;
    private final MDCService mdcService;

    @JmsListener(destination = "${gp2gp.amqp.taskQueueName}")
    @SneakyThrows
    public void receive(Message message) throws JMSException, TaskHandlerException {
        var messageID = message.getJMSMessageID();
        LOGGER.info("Received message from taskQueue {}", messageID);
        try {
            taskHandler.handle(message);
            message.acknowledge();
            LOGGER.info("Acknowledged message {}", messageID);
        } catch (Exception e) {
            LOGGER.error("Error while processing task queue message {}", messageID, e);
        } finally {
            mdcService.resetAllMdcKeys();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
