package uk.nhs.adaptors.gp2gp.mhs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import uk.nhs.adaptors.gp2gp.ResourceHelper;
import uk.nhs.adaptors.gp2gp.ehr.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InboundMessageHandlerTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Spy
    private XPathService xPathService;
    @InjectMocks
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message message;

    @Test
    @SneakyThrows
    public void When_messageIsUnreadable_Then_exceptionIsThrown() {
        doThrow(mock(JMSException.class)).when(message).getBody(String.class);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("Unable to read");
    }

    @Test
    @SneakyThrows
    public void When_messageIsNotJson_Then_exceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn("notJson");
        doThrow(mock(JsonProcessingException.class)).when(objectMapper).readValue("notJson", InboundMessage.class);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("not valid JSON");
    }

    @Test
    @SneakyThrows
    public void When_messageIsEhrExtractRequest_Then_requestHandlerCalled() {
        when(message.getBody(String.class)).thenReturn("inboundMessage");
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML("ebxml");
        inboundMessage.setPayload("payload");
        when(objectMapper.readValue("inboundMessage", InboundMessage.class)).thenReturn(inboundMessage);
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).prepareDocumentFromXml("ebxml");
        doReturn(payload).when(xPathService).prepareDocumentFromXml("payload");

        inboundMessageHandler.handle(message);

        verify(ehrExtractRequestHandler).handle(header, payload);
    }

    @Test
    @SneakyThrows
    public void When_messageIsForUnknownInteraction_Then_exceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn("inboundMessage");
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML("ebxml");
        inboundMessage.setPayload("payload");
        when(objectMapper.readValue("inboundMessage", InboundMessage.class)).thenReturn(inboundMessage);
        Document header = mock(Document.class);
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).prepareDocumentFromXml("ebxml");
        doReturn(payload).when(xPathService).prepareDocumentFromXml("payload");
        doReturn("RCMR_UNKNOWN").when(xPathService).getNodeValue(eq(header), anyString());

        assertThatExceptionOfType(UnsupportedInteractionException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("RCMR_UNKNOWN");

        verifyNoInteractions(ehrExtractRequestHandler);
    }

}
