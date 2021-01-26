package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.IdGenerator;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCore;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;

@ExtendWith(MockitoExtension.class)
public class SendEhrExtractCoreTest {

    private static final String CONVERSATION_ID = IdGenerator.get();
    private static final String TASK_ID = IdGenerator.get();
    private static final String REQUEST_ID = IdGenerator.get();
    private static final String TO_ASID = "200000000359";
    private static final String FROM_ASID = "918999198738";
    private static final String ODS_CODE = "GPC001";

    @Mock
    private TaskDispatcher taskDispatcher;
    @Mock
    private TaskIdService taskIdService;

    private SendEhrExtractCore sendEhrExtractCore;

    @Captor
    private ArgumentCaptor<SendEhrExtractCoreTaskDefinition> sendEhrExtractCoreTaskDefinitionArgumentCaptor;

    @BeforeEach
    public void setUp() {
        when(taskIdService.createNewTaskId()).thenReturn(TASK_ID);

        sendEhrExtractCore = new SendEhrExtractCore(taskDispatcher, taskIdService);
    }

    @Test
    public void When_SendEhrExtractCoreTaskIsCreated_Expect_CorrectDefinitionProvided() {
        sendEhrExtractCore.send(EhrExtractStatus.builder()
            .conversationId(CONVERSATION_ID)
            .ehrRequest(EhrExtractStatus.EhrRequest.builder()
                .requestId(REQUEST_ID)
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .fromOdsCode(ODS_CODE)
                .build())
            .build());

        verify(taskDispatcher).createTask(sendEhrExtractCoreTaskDefinitionArgumentCaptor.capture());

        SendEhrExtractCoreTaskDefinition definition = sendEhrExtractCoreTaskDefinitionArgumentCaptor.getValue();
        assertThat(definition.getTaskId()).isEqualTo(TASK_ID);
        assertThat(definition.getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(definition.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(definition.getToAsid()).isEqualTo(TO_ASID);
        assertThat(definition.getFromAsid()).isEqualTo(FROM_ASID);
        assertThat(definition.getFromOdsCode()).isEqualTo(ODS_CODE);
    }
}
