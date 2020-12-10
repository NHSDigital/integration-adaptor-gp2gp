package uk.nhs.adaptors.gp2gp.testcontainers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.jms.support.destination.JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT;

@Slf4j
public class ActiveMQExtension implements BeforeAllCallback, BeforeEachCallback {

    public static final String DLQ_PREFIX = "DLQ.";
    private JmsTemplate jmsTemplate;

    @Override
    public void beforeAll(ExtensionContext context) {
        ActiveMqContainer.getInstance().start();
    }

    @Override
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "SpotBugs issue with fix not yet released https://github.com/spotbugs/spotbugs/issues/456")
    public void beforeEach(ExtensionContext context) {
        var applicationContext = SpringExtension.getApplicationContext(context);

        jmsTemplate = applicationContext.getBean(JmsTemplate.class);

        var inboundqueueName = Objects.requireNonNull(
                applicationContext.getEnvironment().getProperty("gp2gp.amqp.inboundQueueName"));
        var taskqueueName = Objects.requireNonNull(
                applicationContext.getEnvironment().getProperty("gp2gp.amqp.taskQueueName"));

        var receiveTimeout = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(RECEIVE_TIMEOUT_NO_WAIT);

        List<String> list = new ArrayList();
        if (isNotBlank(inboundqueueName)) {
            list.add(inboundqueueName);
            list.add(DLQ_PREFIX + inboundqueueName);
        }
        if (isNotBlank(taskqueueName)) {
            list.add(taskqueueName);
            list.add(DLQ_PREFIX + taskqueueName);
        }
        for (String name : list) {
            purgeQueueMessage(name);
        }

        jmsTemplate.setReceiveTimeout(receiveTimeout);
    }

    private void purgeQueueMessage(String name) {
        while (jmsTemplate.receive(name) != null) {
            LOGGER.info("Purged '" + name + "' message");
        }
    }
}
