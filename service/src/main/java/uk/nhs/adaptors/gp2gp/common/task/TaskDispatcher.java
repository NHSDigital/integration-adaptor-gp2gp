package uk.nhs.adaptors.gp2gp.common.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDispatcher {

    private final JmsTemplate jmsTemplate;

    @Value("${gp2gp.amqp.taskQueueName}")
    private String mhsTaskQueueName;

    // FIXME: NIAD-761 Remove and replace with actual implementation
    public void temporaryCreateTask(String value) {
        jmsTemplate.send(mhsTaskQueueName, session -> session.createTextMessage(value));
    }

}
