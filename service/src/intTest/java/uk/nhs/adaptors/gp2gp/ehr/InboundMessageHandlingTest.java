package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;

import javax.jms.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessage;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageConsumer;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class InboundMessageHandlingTest {
    private static final String CONTINUE_INTERACTION_ID = "COPC_IN000001UK01";
    private static final String ACTION_PATH = "/Envelope/Header/MessageHeader/Action";
    private static final String CONVERSATION_ID_PATH = "/Envelope/Header/MessageHeader/ConversationId";
    private static final String CONTINUE_MESSAGE_PAYLOAD = "Continue Acknowledgement";

    @MockBean
    private XPathService xPathService;
    @MockBean
    private ObjectMapper objectMapper;
    @MockBean
    private TaskDispatcher taskDispatcher;
    @Autowired
    private InboundMessageConsumer inboundMessageConsumer;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Mock
    private Message message;
    @Mock
    private InboundMessage inboundMessage;

    @Test
    @SneakyThrows
    public void When_MessageIsUnreadable_Expect_MessageProcessingToBeAborted() {
        mockNonJsonMessage();

        inboundMessageConsumer.receive(message);

        verify(message, never()).acknowledge();
        verifyNoInteractions(taskDispatcher);
    }

    @Test
    @SneakyThrows
    public void When_MessageProcessingFails_Expect_WholeProcessToBeFailed() {
        mockInboundMessage(CONTINUE_INTERACTION_ID, CONTINUE_MESSAGE_PAYLOAD);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        doThrow(RuntimeException.class).when(taskDispatcher).createTask(any(SendDocumentTaskDefinition.class));

        inboundMessageConsumer.receive(message);

        verify(message).acknowledge();
        assertThatSendNackTaskHasBeenTriggered();
        assertConversationIsFailed(ehrExtractStatus);
    }

    @Test
    @SneakyThrows
    public void When_ProcessIsAlreadyFailed_Expect_MessageProcessingToBeAborted() {
        mockInboundMessage(CONTINUE_INTERACTION_ID, CONTINUE_MESSAGE_PAYLOAD);

        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setError(EhrExtractStatus.Error.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var initialDbExtract = readEhrExtractStatusFromDb();

        inboundMessageConsumer.receive(message);

        verify(message).acknowledge();
        verifyNoInteractions(taskDispatcher);

        var finalDbExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(finalDbExtract).usingRecursiveComparison().isEqualTo(initialDbExtract);
    }

    @Test
    @SneakyThrows
    public void When_ProcessIsNotFailed_Expect_MessageToBeProcessed() {
        mockInboundMessage(CONTINUE_INTERACTION_ID, CONTINUE_MESSAGE_PAYLOAD);
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatus.setEhrExtractCorePending(EhrExtractStatus.EhrExtractCorePending.builder().build());
        ehrExtractStatusRepository.save(ehrExtractStatus);

        var initialDbExtract = readEhrExtractStatusFromDb();
        assertThat(initialDbExtract.getEhrContinue()).isNull();

        inboundMessageConsumer.receive(message);

        verify(message).acknowledge();
        assertSendDocumentTaskHasBeenTriggered();

        var finalDbExtract = readEhrExtractStatusFromDb();
        assertThat(finalDbExtract.getEhrContinue()).isNotNull();
        assertThat(finalDbExtract.getError()).isNull();
    }

    private void assertConversationIsFailed(EhrExtractStatus ehrExtractStatus) {
        var dbExtract = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();
        assertThat(dbExtract.getError()).isNotNull();
    }

    private void assertThatSendNackTaskHasBeenTriggered() {
        verify(taskDispatcher).createTask(
            argThat(taskDefinition ->
                taskDefinition instanceof SendAcknowledgementTaskDefinition
                    && ((SendAcknowledgementTaskDefinition) taskDefinition).isNack()
            )
        );
    }

    private void assertSendDocumentTaskHasBeenTriggered() {
        var sendDocumentTaskDefinitionCaptor = ArgumentCaptor.forClass(SendDocumentTaskDefinition.class);
        verify(taskDispatcher).createTask(sendDocumentTaskDefinitionCaptor.capture());
    }

    @SneakyThrows
    private void mockNonJsonMessage() {
        doThrow(JsonProcessingException.class)
            .when(objectMapper)
            .readValue(
                ArgumentMatchers.<String>any(),
                ArgumentMatchers.<Class<InboundMessage>>any()
            );
    }

    @SneakyThrows
    private void mockInboundMessage(String interactionId, String payload) {
        when(
            objectMapper.readValue(ArgumentMatchers.<String>any(), ArgumentMatchers.<Class<InboundMessage>>any())
        ).thenReturn(inboundMessage);

        when(xPathService.getNodeValue(any(), eq(ACTION_PATH))).thenReturn(interactionId);
        when(xPathService.getNodeValue(any(), eq(CONVERSATION_ID_PATH))).thenReturn(CONVERSATION_ID);

        when(inboundMessage.getPayload()).thenReturn(payload);
    }

    private EhrExtractStatus readEhrExtractStatusFromDb() {
        return ehrExtractStatusRepository.findByConversationId(CONVERSATION_ID).get();
    }
}
