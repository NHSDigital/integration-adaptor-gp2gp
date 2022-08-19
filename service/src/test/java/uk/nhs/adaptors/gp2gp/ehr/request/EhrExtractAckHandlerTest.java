package uk.nhs.adaptors.gp2gp.ehr.request;

import static javax.xml.xpath.XPathConstants.NODESET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

@ExtendWith(MockitoExtension.class)
public class EhrExtractAckHandlerTest {

    private static final String ACK_TYPE_CODE_XPATH = "//MCCI_IN010000UK13/acknowledgement/@typeCode";
    private static final String MESSAGE_REF_XPATH = "//MCCI_IN010000UK13/acknowledgement/messageRef/id/@root";
    private static final String ERROR_CODE_XPATH = "//justifyingDetectedIssueEvent/code";
    private static final String ACK_DETAILS_XPATH = "//MCCI_IN010000UK13/acknowledgement/acknowledgementDetail/code";
    private static final String ACK_OK_CODE = "AA";
    private static final String ACK_BUSINESS_ERROR_CODE = "AE";
    private static final String ACK_REJECTED_CODE = "AR";
    private static final String CONVERSATION_ID =  "mock-id";
    private static final String EHR_MESSAGE_REF = "mock-ehr-message-ref";
    private static final String RANDOM_MESSAGE_REF = "mock-random-message-ref";
    private static final String ERROR_CODE_ELEMENT_99 = "<code code=\"99\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.17.101\" displayName=\"Unexpected condition\"/>";
    private static final String ERROR_CODE_ELEMENT_18 = "<code code=\"18\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.17.101\" displayName=\"Request message not "
        + "well-formed or not able to be processed\"/>";

    @Mock
    private XPathService xPathService;

    @Mock
    private EhrExtractStatusService ehrExtractStatusService;

    @Mock
    private Document document;

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

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).updateEhrExtractStatusAck(eq(CONVERSATION_ID), receivedAckField.capture());

        var ackFieldValue = receivedAckField.getValue();

        assertThat(ackFieldValue.getConversationClosed()).isNotNull();
    }

    @Test
    public void When_Handle_WithAckAndMessageRefEqualsEhrExtractMessageId_Expect_AckSaved() {

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(eq(CONVERSATION_ID), any());
    }

    @Test
    public void When_Handle_WithAckDoesNotReferenceEhrExtract_Expect_ReceivedAckFieldNotUpdated() {

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(RANDOM_MESSAGE_REF);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService, never()).updateEhrExtractStatusAck(any(), any());
    }

    @Test
    public void When_Handle_WithAckDoesNotReferenceEhrExtract_Expect_AckSaved() {

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(RANDOM_MESSAGE_REF);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(eq(CONVERSATION_ID), any());
    }

    @Test
    public void When_Handle_WithAckAndNoEhrExtractMessageIdForConversation_Expect_EhrExtractException() {

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_OK_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);

        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.empty());

        assertThatExceptionOfType(EhrExtractException.class)
            .isThrownBy(() -> ehrExtractAckHandler.handle(CONVERSATION_ID, document));
    }

    @Test
    public void When_Handle_WithNackReferencesEhrExtract_Expect_ConversationClosed() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_99);
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_BUSINESS_ERROR_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ERROR_CODE_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).updateEhrExtractStatusAck(eq(CONVERSATION_ID), receivedAckField.capture());
        var ackFieldValue = receivedAckField.getValue();
        assertThat(ackFieldValue.getConversationClosed()).isNotNull();
    }

    @Test
    public void When_Handle_WithNackReferencesEhrExtract_Expect_AckSaved() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_99);
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_BUSINESS_ERROR_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ERROR_CODE_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(eq(CONVERSATION_ID), any());
    }

    @Test
    public void When_Handle_WithNackDoesNotReferenceExtract_Expect_ReceivedAckFieldNotUpdated() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_99);

        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_BUSINESS_ERROR_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ERROR_CODE_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(RANDOM_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService, never()).updateEhrExtractStatusAck(any(), any());
    }

    @Test
    public void When_Handle_WithNackDoesNotReferenceExtract_Expect_AckSaved() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_99);

        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_BUSINESS_ERROR_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ERROR_CODE_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(RANDOM_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(any(), any());
    }

    @Test
    public void When_Handle_WithRejectedReferencesEhrExtract_Expect_ConversationClosed() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_18);

        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_REJECTED_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ACK_DETAILS_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).updateEhrExtractStatusAck(eq(CONVERSATION_ID), receivedAckField.capture());
        var ackFieldValue = receivedAckField.getValue();
        assertThat(ackFieldValue.getConversationClosed()).isNotNull();
    }

    @Test
    public void When_Handle_WithRejectedDoesNotReferenceExtract_Expect_ReceivedAckFieldNotUpdated() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_18);
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_REJECTED_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ACK_DETAILS_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(RANDOM_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService, never()).updateEhrExtractStatusAck(any(), any());
    }

    @Test
    public void When_Handle_WithRejectedReferencesEhrExtract_Expect_AckSaved() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_18);

        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_REJECTED_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ACK_DETAILS_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(EHR_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(eq(CONVERSATION_ID),any());

    }

    @Test
    public void When_Handle_WithRejectedDoesNotReferenceExtract_Expect_AckSaved() throws XPathExpressionException,
        ParserConfigurationException, IOException, SAXException {

        NodeList codeNodeList = codeElementToNodeList(ERROR_CODE_ELEMENT_18);
        var document = mock(Document.class);

        when(xPathService.getNodeValue(any(), eq(ACK_TYPE_CODE_XPATH))).thenReturn(ACK_REJECTED_CODE);
        when(xPathService.getNodeValue(any(), eq(MESSAGE_REF_XPATH))).thenReturn(EHR_MESSAGE_REF);
        when(xPathService.getNodes(any(), eq(ACK_DETAILS_XPATH))).thenReturn(codeNodeList);
        when(ehrExtractStatusService.fetchEhrExtractMessageId(eq(CONVERSATION_ID))).thenReturn(Optional.of(RANDOM_MESSAGE_REF));

        ehrExtractAckHandler.handle(CONVERSATION_ID, document);

        verify(ehrExtractStatusService).saveAckForConversation(eq(CONVERSATION_ID), any());
    }

    private NodeList codeElementToNodeList(String element) throws XPathExpressionException, ParserConfigurationException, IOException,
        SAXException {
        Document document = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(element.getBytes()));

        XPathExpression xPathExpression = XPathFactory.newInstance()
            .newXPath()
            .compile("//code");

        return (NodeList) xPathExpression.evaluate(document, NODESET);
    }
}
