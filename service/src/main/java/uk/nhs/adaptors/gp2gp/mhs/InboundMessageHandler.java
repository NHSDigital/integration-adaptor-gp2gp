package uk.nhs.adaptors.gp2gp.mhs;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrRequestHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundMessageHandler {
    private final ObjectMapper objectMapper;
    private final EhrRequestHandler ehrRequestHandler;

    public void handle(Message message) throws JMSException, JsonProcessingException {
        String body = JmsReader.readMessage(message);
        LOGGER.debug("Message content: {}", body);
        var mhsInboundMessage = objectMapper.readValue(body, InboundMessage.class);
        ehrRequestHandler.handleRequest(objectMapper.writeValueAsString(mhsInboundMessage));
    }

}
