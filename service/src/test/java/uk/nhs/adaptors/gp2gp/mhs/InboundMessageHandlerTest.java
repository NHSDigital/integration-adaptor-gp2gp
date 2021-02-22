package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.ResourceHelper;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;

@ExtendWith(MockitoExtension.class)
public class InboundMessageHandlerTest {
    private static final String BODY_NOT_JSON = "notJson";
    private static final String EBXML_CONTENT = "ebxml";
    private static final String PAYLOAD_CONTENT = "payload";
    private static final String INBOUND_MESSAGE_CONTENT = "inboundMessage";
    private static final String UNKNOWN_INTERACTION_ID = "RCMR_UNKNOWN";
    private static final String INVALID_EBXML_CONTENT = "NOT VALID XML";
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Spy
    private XPathService xPathService;
    @Mock
    private MDCService mdcService;
    @InjectMocks
    private InboundMessageHandler inboundMessageHandler;
    @Mock
    private Message message;

    @Test
    @SneakyThrows
    public void When_MessageIsUnreadable_Expect_ExceptionIsThrown() {
        doThrow(mock(JMSException.class)).when(message).getBody(String.class);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("Unable to read");
    }

    @Test
    @SneakyThrows
    public void When_MessageIsNotJson_Expect_ExceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn(BODY_NOT_JSON);
        doThrow(mock(JsonProcessingException.class)).when(objectMapper).readValue(BODY_NOT_JSON, InboundMessage.class);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("not valid JSON");
    }

    @Test
    @SneakyThrows
    public void When_MessageIsEhrExtractRequest_Expect_RequestHandlerCalled() {
        when(message.getBody(String.class)).thenReturn(INBOUND_MESSAGE_CONTENT);
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(EBXML_CONTENT);
        inboundMessage.setPayload(PAYLOAD_CONTENT);
        when(objectMapper.readValue(INBOUND_MESSAGE_CONTENT, InboundMessage.class)).thenReturn(inboundMessage);
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(payload).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);

        inboundMessageHandler.handle(message);

        verify(ehrExtractRequestHandler).handleStart(header, payload);
    }

    @Test
    @SneakyThrows
    public void When_MessageIsForUnknownInteraction_Expect_ExceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn(INBOUND_MESSAGE_CONTENT);
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(EBXML_CONTENT);
        inboundMessage.setPayload(PAYLOAD_CONTENT);
        when(objectMapper.readValue(INBOUND_MESSAGE_CONTENT, InboundMessage.class)).thenReturn(inboundMessage);
        Document header = mock(Document.class);
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(payload).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);
        doReturn(UNKNOWN_INTERACTION_ID).when(xPathService).getNodeValue(eq(header), anyString());

        assertThatExceptionOfType(UnsupportedInteractionException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining(UNKNOWN_INTERACTION_ID);

        verifyNoInteractions(ehrExtractRequestHandler);
    }

    @Test
    @SneakyThrows
    public void When_MessageHeaderCannotBeParsed_Expect_ExceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn(INBOUND_MESSAGE_CONTENT);
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(INVALID_EBXML_CONTENT);
        inboundMessage.setPayload(PAYLOAD_CONTENT);
        when(objectMapper.readValue(INBOUND_MESSAGE_CONTENT, InboundMessage.class)).thenReturn(inboundMessage);
        Document payload = mock(Document.class);
        doCallRealMethod().when(xPathService).parseDocumentFromXml(INVALID_EBXML_CONTENT);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("XML envelope");
    }

    @Test
    @SneakyThrows
    public void When_MessagePayloadCannotBeParsed_Expect_ExceptionIsThrown() {
        when(message.getBody(String.class)).thenReturn(INBOUND_MESSAGE_CONTENT);
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(EBXML_CONTENT);
        inboundMessage.setPayload(PAYLOAD_CONTENT);
        when(objectMapper.readValue(INBOUND_MESSAGE_CONTENT, InboundMessage.class)).thenReturn(inboundMessage);
        Document header = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doCallRealMethod().when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> inboundMessageHandler.handle(message))
            .withMessageContaining("XML payload");
    }

}
