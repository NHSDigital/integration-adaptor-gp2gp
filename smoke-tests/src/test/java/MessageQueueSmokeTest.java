import static org.assertj.core.api.Assertions.fail;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import util.EnvVarsUtil;

public class MessageQueueSmokeTest {

    private static final String BROKERS_ENV_VARIABLE = "GP2GP_AMQP_BROKERS";
    private static final String BROKERS_DEFAULT_VALUE = "amqp://localhost:5672";
    private static final String USERNAME_ENV_VARIABLE = "GP2GP_AMQP_USERNAME";
    private static final String USERNAME_DEFAULT_VALUE = "";
    private static final String PASSWORD_ENV_VARIABLE = "GP2GP_AMQP_PASSWORD";
    private static final String PASSWORD_DEFAULT_VALUE = "";
    private static final String ACTIVEMQ_CONTAINER_NAME = "activemq";

    private static String brokers;
    private static String username;
    private static String password;

    @BeforeAll
    public static void setup() {
        Map<String, String> envVars = System.getenv();

        brokers = envVars.getOrDefault(BROKERS_ENV_VARIABLE, BROKERS_DEFAULT_VALUE);

        brokers = EnvVarsUtil.replaceContainerUri(brokers, "amqp", ACTIVEMQ_CONTAINER_NAME);

        username = envVars.getOrDefault(USERNAME_ENV_VARIABLE, USERNAME_DEFAULT_VALUE);
        password = envVars.getOrDefault(PASSWORD_ENV_VARIABLE, PASSWORD_DEFAULT_VALUE);
    }

    @Test
    public void expect_ActiveMQIsAvailable() {

        JmsConnectionFactory connectionFactory = new JmsConnectionFactory();

        connectionFactory.setRemoteURI(brokers);

        boolean hasUsernameAndPassword = !username.isBlank() && !password.isBlank();

        if (hasUsernameAndPassword) {
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
        }

        try(Connection connection = hasUsernameAndPassword ?
            connectionFactory.createConnection(username, password) :
            connectionFactory.createConnection()) {

            // this will throw if the connection isn't available
            connection.setClientID("test client");

        } catch (JMSException e) {
            fail("Unable to connect to message queue at " + brokers + ". Due to: " + e.getMessage());
        }
    }
}
