package uk.nhs.adaptors.gp2gp.utils;

import java.nio.charset.StandardCharsets;

import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsTextMessage;

import javax.jms.JMSException;
import javax.jms.Message;

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
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String readTextMessage(JmsTextMessage message) throws JMSException {
        return message.getText();
    }
}
