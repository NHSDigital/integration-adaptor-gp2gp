package uk.nhs.adaptors.gp2gp;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class MessageQueue {

    public static void main(String args[]) throws Exception {
        // adapted from https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-java-how-to-use-jms-api-amqp
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("connectionfactory.SBCF", "amqp://localhost:5672");
        hashtable.put("queue.QUEUE", "BasicQueue");
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
        message.setText("Hello World!");

        // should be closed in a finally
        producer.close();
        session.close();
        connection.stop();
        connection.close();
    }

}
