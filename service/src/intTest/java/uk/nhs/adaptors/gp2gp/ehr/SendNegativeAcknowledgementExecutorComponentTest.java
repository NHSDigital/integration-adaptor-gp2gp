package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.common.ResourceReader.asString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SendNegativeAcknowledgementExecutorComponentTest {

    private static final String GENERATED_RANDOM_ID = "GENERATED-RANDOM-ID";
    private static final String FROM_ASID = "0000222-from-asid";
    private static final String TO_ASID = "0000333-to-asid";
    private static final String EHR_REQUEST_MESSAGE_ID = "000-333-444-ehr-request-message-id";
    private static final String DATE = "2018-03-04T03:10:41.01Z";
    private static final String REASON_CODE = "06";
    private static final String REASON_MESSAGE = "Patient not at surgery.";
    private static final String CONVERSATION_ID = "888-000-conversation-id";
    private static final String TASK_ID = "999-000-task-id";
    private static final String TYPE_CODE = "AE";

    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;
    @MockBean
    private MhsClient mhsClient;
    @MockBean
    private TimestampService timestampService;
    @MockBean
    private RandomIdGeneratorService randomIdGeneratorService;
    @MockBean
    private EhrExtractStatusService ehrExtractStatusService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SendNegativeAcknowledgementExecutor executor;

    @Value("classpath:ehr/expected-nack-message.json")
    private Resource expectedMessage;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(GENERATED_RANDOM_ID);
        when(timestampService.now()).thenReturn(Instant.parse(DATE));
    }

    @Test
    public void When_SendingNegativeAcknowledgement_Expect_CorrectRequestBodySent() {
        var taskDefinition =
            SendNegativeAcknowledgementTaskDefinition.builder()
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .ehrRequestMessageId(EHR_REQUEST_MESSAGE_ID)
                .reasonCode(REASON_CODE)
                .detail(REASON_MESSAGE)
                .conversationId(CONVERSATION_ID)
                .taskId(TASK_ID)
                .typeCode(TYPE_CODE)
                .build();

        executor.execute(taskDefinition);

        verify(mhsRequestBuilder).buildSendAcknowledgement(asString(expectedMessage), FROM_ASID, CONVERSATION_ID, GENERATED_RANDOM_ID);

        verify(ehrExtractStatusService).updateEhrExtractStatusNegativeAcknowledgement(taskDefinition, GENERATED_RANDOM_ID);
    }
}
