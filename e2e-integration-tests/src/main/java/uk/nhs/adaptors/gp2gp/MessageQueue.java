package uk.nhs.adaptors.gp2gp;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class MessageQueue {

    public static void sendToMhsInboundQueue(String messageContent) throws Exception {
        String broker = System.getenv().getOrDefault("GP2GP_AMQP_BROKERS", "amqp://localhost:5672");
        String queueName = System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "inbound");

        // adapted from https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-java-how-to-use-jms-api-amqp
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("connectionfactory.SBCF", broker);
        hashtable.put("queue.QUEUE", queueName);
        hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        Context context = new InitialContext(hashtable);

        ConnectionFactory cf = (ConnectionFactory) context.lookup("SBCF");

        // Look up queue
        Destination queue = (Destination) context.lookup("QUEUE");

        // Create connection
        Connection connection = cf.createConnection(); // Azure uses params (csb.getSasKeyName(), csb.getSasKey())
        // Create session, no transaction, client ack
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        // Create producer
        MessageProducer producer = session.createProducer(queue);

        // Send message
        TextMessage message = session.createTextMessage();
        message.setText(messageContent);
        producer.send(message);

        // should be closed in a finally
        producer.close();
        session.close();
        connection.stop();
        connection.close();
    }

}
