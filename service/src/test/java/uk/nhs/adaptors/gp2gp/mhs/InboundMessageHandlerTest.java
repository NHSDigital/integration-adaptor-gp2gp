package uk.nhs.adaptors.gp2gp.mhs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.ResourceHelper;
import uk.nhs.adaptors.gp2gp.common.service.MDCService;
import uk.nhs.adaptors.gp2gp.common.service.ProcessFailureHandlingService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.request.EhrExtractRequestHandler;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnsupportedInteractionException;

@ExtendWith(MockitoExtension.class)
public class InboundMessageHandlerTest {
    private static final String BODY_NOT_JSON = "notJson";
    private static final String EBXML_CONTENT = "ebxml";
    private static final String PAYLOAD_CONTENT = "payload";
    private static final String INBOUND_MESSAGE_CONTENT = "inboundMessage";
    private static final String UNKNOWN_INTERACTION_ID = "RCMR_UNKNOWN";
    private static final String INVALID_EBXML_CONTENT = "NOT VALID XML";
    private static final String NACK_ERROR_CODE = "18";

    // conversation ID from the header of the valid message
    private static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EhrExtractRequestHandler ehrExtractRequestHandler;
    @Mock
    private ProcessFailureHandlingService processFailureHandlingService;
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
    public void When_MessageIsUnreadable_Expect_MessageProcessingToBeAborted() {
        doThrow(mock(JMSException.class)).when(message).getBody(String.class);

        var result = inboundMessageHandler.handle(message);
        assertThat(result).isFalse();

        verifyNoInteractions(ehrExtractRequestHandler, mdcService, processFailureHandlingService);
    }

    @Test
    @SneakyThrows
    public void When_MessageIsNotJson_Expect_MessageProcessingToBeAborted() {
        when(message.getBody(String.class)).thenReturn(BODY_NOT_JSON);
        doThrow(mock(JsonProcessingException.class)).when(objectMapper).readValue(BODY_NOT_JSON, InboundMessage.class);

        var result = inboundMessageHandler.handle(message);
        assertThat(result).isFalse();

        verifyNoInteractions(ehrExtractRequestHandler, mdcService, processFailureHandlingService);
    }

    @Test
    @SneakyThrows
    public void When_ExceptionIsThrownWhenParsingTheMessage_Expect_MessageProcessingToBeAborted() {
        setUpEhrExtract(EBXML_CONTENT);
        doThrow(RuntimeException.class).when(xPathService).parseDocumentFromXml(any());

        var result = inboundMessageHandler.handle(message);

        assertThat(result).isFalse();
        verifyNoInteractions(ehrExtractRequestHandler, mdcService, processFailureHandlingService);
    }

    @Test
    @SneakyThrows
    public void When_MessageIsEhrExtractRequest_Expect_RequestHandlerCalled() {
        setUpEhrExtract(EBXML_CONTENT);
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(payload).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);

        var result = inboundMessageHandler.handle(message);

        assertThat(result).isTrue();
        verify(ehrExtractRequestHandler).handleStart(header, payload);
    }

    @Test
    @SneakyThrows
    public void When_MessageIsEhrExtractRequestAck_Expect_RequestAckHandlerCalled() {
        setUpEhrExtract(EBXML_CONTENT);
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/MCCI_IN010000UK13_header.xml");
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(payload).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);

        var result = inboundMessageHandler.handle(message);

        assertThat(result).isTrue();
        verify(ehrExtractRequestHandler).handleAcknowledgement("75049C80-5271-11EA-9384-E83935108FD5", payload);
    }

    @Test
    @SneakyThrows
    public void When_MessageIsForUnknownInteraction_Expect_ExceptionIsThrown() {
        setUpEhrExtract(EBXML_CONTENT);
        Document header = mock(Document.class);
        Document payload = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(payload).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);
        doReturn(UNKNOWN_INTERACTION_ID).when(xPathService).getNodeValue(eq(header), anyString());

        Exception exception = assertThrows(UnsupportedInteractionException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Unsupported interaction id RCMR_UNKNOWN");

        verifyNoInteractions(ehrExtractRequestHandler);
    }

    @Test
    @SneakyThrows
    public void When_MessageHeaderCannotBeParsed_Expect_FalseToBeReturned() {
        setUpEhrExtract(INVALID_EBXML_CONTENT);
        doCallRealMethod().when(xPathService).parseDocumentFromXml(INVALID_EBXML_CONTENT);

        var result = inboundMessageHandler.handle(message);
        assertThat(result).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_MessagePayloadCannotBeParsed_Expect_FalseToBeReturned() {
        setUpEhrExtract(EBXML_CONTENT);
        Document header = mock(Document.class);
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doCallRealMethod().when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);

        var result = inboundMessageHandler.handle(message);
        assertThat(result).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_MessageHandlingFails_Expect_ProcessToBeFailed() {
        setupValidMessage();

        doThrow(new RuntimeException("test exception")).when(ehrExtractRequestHandler).handleStart(any(), any());
        doReturn(true).when(processFailureHandlingService).failProcess(any(), any(), any(), any());

        var result = inboundMessageHandler.handle(message);
        assertThat(result).isTrue();

        verify(processFailureHandlingService).failProcess(
            CONVERSATION_ID,
            NACK_ERROR_CODE,
            "There has been an error when processing the message",
            "InboundMessageHandler"
        );
    }

    @Test
    @SneakyThrows
    public void When_MessageProcessingFails_Expect_ResultFromFailureHandlerToBeReturned() {
        setupValidMessage();

        doThrow(new RuntimeException("test exception")).when(ehrExtractRequestHandler).handleStart(any(), any());

        doReturn(true, false).when(processFailureHandlingService).failProcess(any(), any(), any(), any());

        assertThat(inboundMessageHandler.handle(message)).isTrue();
        assertThat(inboundMessageHandler.handle(message)).isFalse();
    }

    @Test
    @SneakyThrows
    public void When_ErrorHandlingFails_Expect_ExceptionThrown() {
        setupValidMessage();

        doThrow(new RuntimeException("message handling exception")).when(ehrExtractRequestHandler).handleStart(any(), any());

        var failureHandlingException = new RuntimeException("failure handling exception");
        doThrow(failureHandlingException).when(processFailureHandlingService).failProcess(any(), any(), any(), any());

        assertThatThrownBy(() -> inboundMessageHandler.handle(message)).isSameAs(failureHandlingException);
    }

    @Test
    @SneakyThrows
    public void When_ProcessHasAlreadyFailed_Expect_TaskNotExecuted() {
        setupValidMessage();
        doReturn(true).when(processFailureHandlingService).hasProcessFailed(any());

        var result = inboundMessageHandler.handle(message);

        assertThat(result).isTrue();

        verify(processFailureHandlingService).hasProcessFailed(CONVERSATION_ID);
        verifyNoInteractions(ehrExtractRequestHandler);
    }

    private void setupValidMessage() throws JMSException, JsonProcessingException, SAXException {
        setUpEhrExtract(EBXML_CONTENT);
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        doReturn(header).when(xPathService).parseDocumentFromXml(EBXML_CONTENT);
        doReturn(mock(Document.class)).when(xPathService).parseDocumentFromXml(PAYLOAD_CONTENT);
    }

    private void setUpEhrExtract(String ebxmlContent) throws JMSException, JsonProcessingException {
        when(message.getBody(String.class)).thenReturn(INBOUND_MESSAGE_CONTENT);
        var inboundMessage = new InboundMessage();
        inboundMessage.setEbXML(ebxmlContent);
        inboundMessage.setPayload(PAYLOAD_CONTENT);
        when(objectMapper.readValue(INBOUND_MESSAGE_CONTENT, InboundMessage.class)).thenReturn(inboundMessage);
    }
}
