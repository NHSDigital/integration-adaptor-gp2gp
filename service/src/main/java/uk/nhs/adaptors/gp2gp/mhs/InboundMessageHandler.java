package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrRequestHandler;

import javax.jms.JMSException;
import javax.jms.Message;

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
        // TODO: NIAD-776 if the inbound message is an EhrRequest interaction then use the ehrRequestHandler
        ehrRequestHandler.handleRequest(objectMapper.writeValueAsString(mhsInboundMessage));
        // TODO: NIAD-776 else we don't know how to handle the message, this is an error
    }

}
