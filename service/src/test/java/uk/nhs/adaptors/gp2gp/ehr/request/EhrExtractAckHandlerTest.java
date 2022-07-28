package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

@ExtendWith(MockitoExtension.class)
public class EhrExtractAckHandlerTest {

    private static final String ACK_TYPE_CODE_XPATH = "//MCCI_IN010000UK13/acknowledgement/@typeCode";
    private static final String MESSAGE_REF_XPATH = "//MCCI_IN010000UK13/acknowledgement/messageRef/id/@root";
    private static final String ACK_OK_CODE = "AA";

    @Mock
    private XPathService xPathService;

    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @Captor
    private ArgumentCaptor<EhrExtractStatus.EhrReceivedAcknowledgement> receivedAckField;

    @InjectMocks
    private EhrExtractAckHandler ehrExtractAckHandler;

    @Test
    public void When_HandleUnsupportedAckTypeCode_Expect_ExceptionThrown() {
        var document = mock(Document.class);
        var messageRef = "mock-message-ref";
        when(xPathService.getNodeValue(document, ACK_TYPE_CODE_XPATH)).thenReturn("CE");
        when(ehrExtractStatusService.fetchEhrExtractMessageId(any())).thenReturn(Optional.of(messageRef));

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> ehrExtractAckHandler.handle("123", document))
            .withMessage("Unsupported //MCCI_IN010000UK13/acknowledgement/@typeCode: CE");
    }

    @Test
    public void When_Handle_WithAckAndMessageRefEqualsEhrExtractMessageId_Expect_ConversationIsClosed() {
        String conversationId = "mock-id";
        String messageRef = "mock-message-ref";
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(messageRef);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(conversationId))).thenReturn(Optional.of(messageRef));

        ehrExtractAckHandler.handle(conversationId, document);

        verify(ehrExtractStatusService).updateEhrExtractStatusAck(eq(conversationId), receivedAckField.capture());

        var ackFieldValue = receivedAckField.getValue();

        assertThat(ackFieldValue.getConversationClosed()).isNotNull();
    }

    @Test
    public void When_Handle_WithAckDoesNotReferenceEhrExtract_Expect_ReceivedAckFieldNotUpdated() {
        String conversationId = "mock-id";
        String ehrMessageRef = "mock-message-ref";
        String randomMessageRef = "mock-random-message-ref";
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(ehrMessageRef);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(conversationId))).thenReturn(Optional.of(randomMessageRef));

        ehrExtractAckHandler.handle(conversationId, document);

        verify(ehrExtractStatusService, never()).updateEhrExtractStatusAck(any(), any());
    }

    @Test
    public void When_Handle_WithAckAndNoEhrExtractMessageIdForConversation_Expect_EhrExtractException() {

        String conversationId = "mock-id";
        String ehrMessageRef = "mock-message-ref";
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(ehrMessageRef);

        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(conversationId))).thenReturn(Optional.empty());

        assertThatExceptionOfType(EhrExtractException.class)
            .isThrownBy(() -> ehrExtractAckHandler.handle(conversationId, document));
    }
}
