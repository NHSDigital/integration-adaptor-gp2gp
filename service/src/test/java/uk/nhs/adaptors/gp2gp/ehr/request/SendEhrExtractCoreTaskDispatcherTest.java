package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDefinition;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@ExtendWith(MockitoExtension.class)
public class SendEhrExtractCoreTaskDispatcherTest {

    private static final String CONVERSATION_ID = "bc96655f-1b24-4772-ace6-c31f4d80ff58";
    private static final String TASK_ID = "3a5009e0-55a1-4eac-9436-894494fa2589";
    private static final String REQUEST_ID = "0c812331-fb6e-4c67-b6d5-9d99022e559f";
    private static final String TO_ASID = "200000000359";
    private static final String FROM_ASID = "918999198738";
    private static final String ODS_CODE = "GPC001";

    @Mock
    private TaskDispatcher taskDispatcher;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private SendEhrExtractCoreTaskDispatcher sendEhrExtractCoreTaskDispatcher;

    @Captor
    private ArgumentCaptor<SendEhrExtractCoreTaskDefinition> sendEhrExtractCoreTaskDefinitionArgumentCaptor;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TASK_ID);

        sendEhrExtractCoreTaskDispatcher = new SendEhrExtractCoreTaskDispatcher(taskDispatcher, randomIdGeneratorService);
    }

    @Test
    public void When_SendEhrExtractCoreTaskIsCreated_Expect_CorrectDefinitionProvided() {
        sendEhrExtractCoreTaskDispatcher.send(EhrExtractStatus.builder()
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
