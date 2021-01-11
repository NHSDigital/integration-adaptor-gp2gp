package uk.nhs.adaptors.mockmhsservice.producer;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class InboundProducer {
    public static void sendToMhsInboundQueue(String messageContent) throws Exception {
        String broker = System.getenv().getOrDefault("GP2GP_AMQP_BROKERS", "amqp://localhost:5672");
        String queueName = System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "inbound");

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("connectionfactory.CF", broker);
        hashtable.put("queue.QUEUE", queueName);
        hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        Context context = new InitialContext(hashtable);

        ConnectionFactory cf = (ConnectionFactory) context.lookup("CF");
        Destination queue = (Destination) context.lookup("QUEUE");
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(queue);
        TextMessage message = session.createTextMessage();
        message.setText(messageContent);
        producer.send(message);

        producer.close();
        session.close();
        connection.stop();
        connection.close();
    }

}
