package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendAcknowledgementTaskDispatcherTest {

    private static final String CONVERSATION_ID = "bc96655f-1b24-4772-ace6-c31f4d80ff58";
    private static final String REQUEST_ID = "0c812331-fb6e-4c67-b6d5-9d99022e559f";
    private static final String MESSAGE_ID = "a407194a-1a15-41ff-9dc6-72b92739c422";
    private static final String TO_ASID = "200000000359";
    private static final String FROM_ASID = "918999198738";
    private static final String FROM_ODS_CODE = "GPC001";
    private static final String TO_ODS_CODE = "GPC002";
    private static final String NACK_TYPE_CODE = "AE";

    @Mock
    private TaskDispatcher taskDispatcher;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @Captor
    private ArgumentCaptor<SendAcknowledgementTaskDefinition> taskDefinitionArgumentCaptor;

    private SendAcknowledgementTaskDispatcher sendNackTaskDispatcher;

    @BeforeEach
    public void setUp() {
        this.sendNackTaskDispatcher = new SendAcknowledgementTaskDispatcher(
            taskDispatcher,
            randomIdGeneratorService
        );
    }

    @Test
    public void When_Called_Expect_TaskTriggeredWithCorrectDefinitionProvided() {
        // ARRANGE
        var reasonCode = "reasonCode1";
        var reasonMessage = "reasonMessage1";

        var taskId = "f41fa0a6-0529-4451-b15d-38584e9f0080";
        when(randomIdGeneratorService.createNewId()).thenReturn(taskId);

        // ACT
        sendNackTaskDispatcher.sendNegativeAcknowledgement(sampleEhrExtractStatus(), reasonCode, reasonMessage);

        // ASSERT
        verify(taskDispatcher).createTask(taskDefinitionArgumentCaptor.capture());

        SendAcknowledgementTaskDefinition definition = taskDefinitionArgumentCaptor.getValue();

        assertThat(definition.getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(definition.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(definition.getToAsid()).isEqualTo(TO_ASID);
        assertThat(definition.getFromAsid()).isEqualTo(FROM_ASID);
        assertThat(definition.getToOdsCode()).isEqualTo(TO_ODS_CODE);
        assertThat(definition.getFromOdsCode()).isEqualTo(FROM_ODS_CODE);
        assertThat(definition.getTaskType()).isEqualTo(TaskType.SEND_ACKNOWLEDGEMENT);
        assertThat(definition.getEhrRequestMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(definition.getTypeCode()).isEqualTo(NACK_TYPE_CODE);

        assertThat(definition.getReasonCode()).isEqualTo(reasonCode);
        assertThat(definition.getDetail()).isEqualTo(reasonMessage);
        assertThat(definition.getTaskId()).isEqualTo(taskId);
        assertThat(definition.isNack()).isTrue();
    }

    private EhrExtractStatus sampleEhrExtractStatus() {
        return EhrExtractStatus.builder()
            .conversationId(CONVERSATION_ID)
            .ehrRequest(EhrExtractStatus.EhrRequest.builder()
                .requestId(REQUEST_ID)
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .fromOdsCode(FROM_ODS_CODE)
                .toOdsCode(TO_ODS_CODE)
                .messageId(MESSAGE_ID)
                .build())
            .build();
    }
}
