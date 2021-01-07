package uk.nhs.adaptors.gp2gp.common.task;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    private static final String TASK_TYPE_HEADER_NAME = "TaskName";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;

    public void handle(Message message) throws JMSException, IOException, TaskHandlerException, ParserConfigurationException, SAXException, XPathExpressionException {
        var taskType = message.getStringProperty(TASK_TYPE_HEADER_NAME);
        var body = JmsReader.readMessage(message);
        LOGGER.info("Current task handled from internal task queue {}", taskType);

        TaskDefinition taskDefinition = taskDefinitionFactory.getTaskDefinition(taskType, body);

        TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.getClass());

        taskExecutor.execute(taskDefinition);
    }
}
