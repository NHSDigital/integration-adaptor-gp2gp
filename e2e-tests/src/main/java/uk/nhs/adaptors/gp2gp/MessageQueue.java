package uk.nhs.adaptors.gp2gp;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MessageQueue {
    public static void sendToMhsInboundQueue(String messageContent) throws NamingException, JMSException {
        Context context = prepareContext(System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "inbound"));

        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("CF");
        Destination queue = (Destination) context.lookup("QUEUE");
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(queue);
        TextMessage message = session.createTextMessage();
        message.setText(messageContent);
        producer.send(message);

        producer.close();
        session.close();
        connection.close();
    }

    public static void sendToMhsTaskQueue(String messageContent) throws NamingException, JMSException {
        Context context = prepareContext(System.getenv().getOrDefault("GP2GP_TASK_QUEUE", "gp2gpTaskQueue"));

        ConnectionFactory cf = (ConnectionFactory) context.lookup("CF");
        Destination queue = (Destination) context.lookup("QUEUE");
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(queue);
        TextMessage message = session.createTextMessage();
        message.setStringProperty("TaskType", "uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition");
        message.setText(messageContent);
        producer.send(message);

        producer.close();
        session.close();
        connection.close();
    }

    private static String prepareBroker() {
        return System.getenv().getOrDefault("GP2GP_AMQP_BROKERS", "amqp://localhost:5672");
    }

    private static Context prepareContext(String queueName) throws NamingException {
        String broker = prepareBroker();

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("connectionfactory.CF", broker);
        hashtable.put("queue.QUEUE", queueName);
        hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        return new InitialContext(hashtable);
    }
}
