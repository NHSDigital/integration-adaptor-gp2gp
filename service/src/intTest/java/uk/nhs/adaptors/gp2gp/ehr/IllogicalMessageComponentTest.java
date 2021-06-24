package uk.nhs.adaptors.gp2gp.ehr;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import javax.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.mhs.exception.MessageOutOfOrderException;
import uk.nhs.adaptors.gp2gp.mhs.exception.NonExistingInteractionIdException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageHandler;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnsupportedInteractionException;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class IllogicalMessageComponentTest {
    private static final String INTERACTION_ID_PATH = "/RCMR_IN010000UK05";
    private static final String SUBJECT_PATH = INTERACTION_ID_PATH + "/ControlActEvent/subject";
    private static final String MESSAGE_HEADER_PATH = "/Envelope/Header/MessageHeader";
    private static final String REQUEST_ID_PATH = SUBJECT_PATH + "/EhrRequest/id/@root";
    private static final String NHS_NUMBER_PATH = SUBJECT_PATH + "/EhrRequest/recordTarget/patient/id/@extension";
    private static final String FROM_PARTY_ID_PATH = MESSAGE_HEADER_PATH + "/From/PartyId";
    private static final String TO_PARTY_ID_PATH = MESSAGE_HEADER_PATH + "/To/PartyId";
    private static final String FROM_ASID_PATH = INTERACTION_ID_PATH + "/communicationFunctionSnd/device/id/@extension";
    private static final String TO_ASID_PATH = INTERACTION_ID_PATH + "/communicationFunctionRcv/device/id/@extension";
    private static final String FROM_ODS_CODE_PATH = SUBJECT_PATH + "/EhrRequest/author/AgentOrgSDS/agentOrganizationSDS/id/@extension";
    private static final String TO_ODS_CODE_PATH = SUBJECT_PATH + "/EhrRequest/destination/AgentOrgSDS/agentOrganizationSDS/id/@extension";
    private static final String MESSAGE_ID_PATH = MESSAGE_HEADER_PATH + "/MessageData/MessageId";
    private static final String EHR_EXTRACT_REQUEST = "RCMR_IN010000UK05";
    private static final String CONTINUE_REQUEST = "COPC_IN000001UK01";
    private static final String ACKNOWLEDGMENT_REQUEST = "MCCI_IN010000UK13";
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";
    private static final String CONVERSATION_ID_PATH = "/Envelope/Header/MessageHeader/ConversationId";
    private static final String NON_EXISTING_CONVERSATION_ID = "d3746650-096e-414b-92a4-146ceaf74f0e";
    private static final String ACK_OK_CODE = "AA";
    private static final XPathService SERVICE = new XPathService();

    @MockBean
    private XPathService xPathService;
    @MockBean
    private ObjectMapper objectMapper;
    @MockBean
    private TaskDispatcher taskDispatcher;
    @Autowired
    private InboundMessageHandler inboundMessageHandler;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private Message message;
    @Mock
    private InboundMessage inboundMessage;

    @Value("classpath:illogicalmessage/RCMR_IN010000UK05_ebxml.txt")
    private Resource requestResponseEbxml;
    @Value("classpath:illogicalmessage/RCMR_IN010000UK05_payload.txt")
    private Resource requestResponsePayload;
    @Value("classpath:illogicalmessage/COPC_IN000001UK01_ebxml.txt")
    private Resource continueResponseEbxml;
    @Value("classpath:illogicalmessage/COPC_IN000001UK01_payload.txt")
    private Resource continueResponsePayload;
    @Value("classpath:illogicalmessage/MCCI_IN010000UK13_ebxml.txt")
    private Resource acknowledgementResponseEbxml;
    @Value("classpath:illogicalmessage/MCCI_IN010000UK13_payload.txt")
    private Resource acknowledgementResponsePayload;

    @Test
    public void When_ContinueRecievedToNonExistingEhrExtractStatus_Expect_ErrorThrown() {
        String continuePayload = asString(continueResponsePayload);
        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml, continuePayload, CONTINUE_REQUEST, NON_EXISTING_CONVERSATION_ID);

        Exception exception = assertThrows(NonExistingInteractionIdException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Received a Continue message that is not recognised with Conversation-Id: 'd3746650-096e-414b-92a4-146ceaf74f0e'");
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_AcknowledgementRecievedtToNonExistingEhrExtractStatus_Expect_ErrorThrown() {
        String acknowledgementPayload = asString(acknowledgementResponsePayload);
        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml, acknowledgementPayload, ACKNOWLEDGMENT_REQUEST, NON_EXISTING_CONVERSATION_ID);

        Exception exception = assertThrows(NonExistingInteractionIdException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Received a ACK message that is not recognised with Conversation-Id: 'd3746650-096e-414b-92a4-146ceaf74f0e'");
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_ContinueReceivedOutOfOrderExtractCoreNotSent_Expect_ErrorThrown() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String continuePayload = asString(continueResponsePayload);
        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml, continuePayload, CONTINUE_REQUEST, ehrExtractStatus.getConversationId());

        Exception exception = assertThrows(MessageOutOfOrderException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Received a Continue message that is out of order in message process with a Conversation-Id: '"
                + ehrExtractStatus.getConversationId() + "'");
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_AcknowledgementReceivedOutOfOrderAcknowledgmentNotSent_Expect_ErrorThrown() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String acknowledgementPayload = asString(acknowledgementResponsePayload);
        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml, acknowledgementPayload, ACKNOWLEDGMENT_REQUEST,
            ehrExtractStatus.getConversationId());

        Exception exception = assertThrows(MessageOutOfOrderException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Received a ACK message that is out of order in message process with a Conversation-Id: '"
                + ehrExtractStatus.getConversationId() + "'");
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateEhrRequestRecieved_Expect_SkippedNoDatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String requestPayload = asString(requestResponsePayload);
        String requestEbxml = asString(requestResponseEbxml);

        mockEhrRequest(requestEbxml, requestPayload, EHR_EXTRACT_REQUEST, ehrExtractStatus.getConversationId());

        inboundMessageHandler.handle(message);
        var firstEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);
        var secondEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateContinueRecieved_Expect_SkippedNoDatabaseUpdated() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatus.setEhrContinue(EhrExtractStatus.EhrContinue.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String continuePayload = asString(continueResponsePayload);
        String continueEbxml = asString(continueResponseEbxml);

        mockIncomingMessage(continueEbxml, continuePayload, CONTINUE_REQUEST, ehrExtractStatus.getConversationId());

        inboundMessageHandler.handle(message);
        var firstEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);
        var secondEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    public void When_DuplicateAcknowledgementSentTwice_Expect_SkippedNoDatabaseUpdatedn() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus(randomIdGeneratorService.createNewId());
        ehrExtractStatus.setAckToRequester(EhrExtractStatus.AckToRequester.builder().build());
        ehrExtractStatus.setEhrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        String acknowledgementPayload = asString(acknowledgementResponsePayload);
        String acknowledgementEbxml = asString(acknowledgementResponseEbxml);

        mockAcknowledgementMessage(acknowledgementEbxml, acknowledgementPayload, ACKNOWLEDGMENT_REQUEST,
            ehrExtractStatus.getConversationId());

        inboundMessageHandler.handle(message);
        var firstEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        inboundMessageHandler.handle(message);
        var secondEhrStatus = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(firstEhrStatus.getUpdatedAt()).isEqualTo(secondEhrStatus.getUpdatedAt());
        verify(taskDispatcher, never()).createTask(any());
    }

    @Test
    @SneakyThrows
    public void When_UnsupportedMessageSent_Expect_ErrorThrown() {
        String incomingMessage = null;
        when(objectMapper.readValue(incomingMessage, InboundMessage.class)).thenReturn(inboundMessage);

        Exception exception = assertThrows(UnsupportedInteractionException.class,
            () -> inboundMessageHandler.handle(message));

        assertThat(exception.getMessage())
            .isEqualTo("Unsupported interaction id null");
        verify(taskDispatcher, never()).createTask(any());
    }

    @SneakyThrows
    private void mockEhrRequest(String ebxml, String payload, String interactionId, String conversationId) {
        String incomingMessage = null;
        when(objectMapper.readValue(incomingMessage, InboundMessage.class)).thenReturn(inboundMessage);
        when(inboundMessage.getEbXML()).thenReturn(ebxml);
        when(inboundMessage.getPayload()).thenReturn(payload);

        var ebxmlDocument = SERVICE.parseDocumentFromXml(ebxml);
        when(xPathService.parseDocumentFromXml(inboundMessage.getEbXML())).thenReturn(ebxmlDocument);
        var payloadDocument = SERVICE.parseDocumentFromXml(payload);
        when(xPathService.parseDocumentFromXml(inboundMessage.getPayload())).thenReturn(payloadDocument);

        when(xPathService.getNodeValue(ebxmlDocument, ACTION_PATH)).thenReturn(interactionId);
        when(xPathService.getNodeValue(ebxmlDocument, CONVERSATION_ID_PATH)).thenReturn(conversationId);

        when(xPathService.getNodeValue(payloadDocument, REQUEST_ID_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(payloadDocument, NHS_NUMBER_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(ebxmlDocument, FROM_PARTY_ID_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(ebxmlDocument, TO_PARTY_ID_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(payloadDocument, FROM_ASID_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(payloadDocument, TO_ASID_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(payloadDocument, FROM_ODS_CODE_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(payloadDocument, TO_ODS_CODE_PATH)).thenReturn("123");
        when(xPathService.getNodeValue(ebxmlDocument, MESSAGE_ID_PATH)).thenReturn("123");
    }

    private static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SneakyThrows
    private void mockIncomingMessage(String ebxml, String payload, String interactionId, String conversationId) {
        String incomingMessage = null;
        when(objectMapper.readValue(incomingMessage, InboundMessage.class)).thenReturn(inboundMessage);
        when(inboundMessage.getEbXML()).thenReturn(ebxml);
        when(inboundMessage.getPayload()).thenReturn(payload);

        var ebxmlDocument = SERVICE.parseDocumentFromXml(ebxml);
        when(xPathService.parseDocumentFromXml(inboundMessage.getEbXML())).thenReturn(ebxmlDocument);
        when(xPathService.getNodeValue(ebxmlDocument, ACTION_PATH)).thenReturn(interactionId);
        when(xPathService.getNodeValue(ebxmlDocument, CONVERSATION_ID_PATH)).thenReturn(conversationId);
    }

    @SneakyThrows
    private void mockAcknowledgementMessage(String ebxml, String payload, String interactionId, String conversationId) {
        when(xPathService.getNodeValue(any(), any())).thenReturn(ACK_OK_CODE);
        mockIncomingMessage(ebxml, payload, interactionId, conversationId);
    }
}
