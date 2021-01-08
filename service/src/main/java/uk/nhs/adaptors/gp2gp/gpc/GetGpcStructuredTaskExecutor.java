package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {

    private final GpcService gpcService;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, TaskHandlerException {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");
        gpcService.handleStructureTask(structuredTaskDefinition);
    }
}
