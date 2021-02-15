package uk.nhs.adaptors.gp2gp.common.amqp;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsTextMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JmsReader {

    public static String readMessage(Message message) throws JMSException {
        if (message instanceof JmsTextMessage) {
            return readTextMessage((JmsTextMessage) message);
        }
        if (message instanceof JmsBytesMessage) {
            return readBytesMessage((JmsBytesMessage) message);
        }
        if (message != null) {
            return message.getBody(String.class);
        }
        return null;
    }

    private static String readBytesMessage(JmsBytesMessage message) throws JMSException {
        byte[] bytes = new byte[(int) message.getBodyLength()];
        message.readBytes(bytes);
        return new String(bytes, UTF_8);
    }

    private static String readTextMessage(JmsTextMessage message) throws JMSException {
        return message.getText();
    }
}
