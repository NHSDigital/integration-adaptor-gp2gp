package uk.nhs.adaptors.gp2gp.common.task;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

public interface TaskExecutor<T extends TaskDefinition> {

    Class<T> getTaskType();

    void execute(T taskDefinition) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, TaskHandlerException;
}
