package uk.nhs.adaptors.gp2gp;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class MessageQueue {

    public static void sendToMhsInboundQueue(String messageContent) throws Exception {
        String broker = System.getenv().getOrDefault("GP2GP_AMQP_BROKERS", "amqp://localhost:5672");
        String queueName = System.getenv().getOrDefault("GP2GP_MHS_INBOUND_QUEUE", "gp2gpTaskQueue");

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
        //message.setText(messageContent);
        message.setStringProperty("TaskName", "StructuredTask");
        message.setText("{\"taskId\":\"21\",\"requestId\":\"21\",\"conversationId\":\"21\",\"nhsNumber\":\"21\"}");
        producer.send(message);

        producer.close();
        session.close();
        connection.stop();
        connection.close();
    }

}
