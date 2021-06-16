package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.ResourceReader.asString;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.MhsClient;
import uk.nhs.adaptors.gp2gp.mhs.MhsRequestBuilder;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@RunWith(SpringRunner.class)
@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class, MockitoExtension.class})
@SpringBootTest
@DirtiesContext
public class SendAcknowledgementComponentTest {
    private static final String GENERATED_RANDOM_ID = "GENERATED-RANDOM-ID";
    private static final String FROM_ASID = "0000222-from-asid";
    private static final String TO_ASID = "0000333-to-asid";
    private static final String FROM_ODS_CODE = "0000222-from-ods-code";
    private static final String TO_ODS_CODE = "0000333-to-ods-code";
    private static final String EHR_REQUEST_MESSAGE_ID = "000-333-444-ehr-request-message-id";
    private static final String DATE = "2018-03-04T03:10:41.01Z";
    private static final String REASON_CODE = "06";
    private static final String REASON_MESSAGE = "Patient not at surgery.";
    private static final String TASK_ID = "999-000-task-id";
    private static final String NEGATIVE_ACK_TYPE_CODE = "AE";
    private static final String POSITIVE_ACK_TYPE_CODE = "AA";

    @MockBean
    private WebClient.RequestHeadersSpec<?> request;
    @MockBean
    private MhsRequestBuilder mhsRequestBuilder;
    @MockBean
    private MhsClient mhsClient;
    @MockBean
    private SendAcknowledgementTaskDefinition sendAcknowledgementTaskDefinition;
    @Autowired
    private SendAcknowledgementExecutor sendAcknowledgementExecutor;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @MockBean
    private TimestampService timestampService;
    @MockBean
    private RandomIdGeneratorService randomIdGeneratorService;

    @Value("classpath:ehr/expected-nack-message.json")
    private Resource expectedNackMessage;

    @Value("classpath:ehr/expected-ack-message.json")
    private Resource expectedAckMessage;

    private EhrExtractStatus ehrExtractStatus;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(GENERATED_RANDOM_ID);
        when(timestampService.now()).thenReturn(Instant.parse(DATE));

        ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    @Test
    public void When_AcknowledgementTaskRunsTwice_Expect_DatabaseOverwritesEhrExtractStatus() {
        var ehrRequest = ehrExtractStatus.getEhrRequest();

        when(sendAcknowledgementTaskDefinition.getFromAsid()).thenReturn(ehrRequest.getFromAsid());
        when(sendAcknowledgementTaskDefinition.getToAsid()).thenReturn(ehrRequest.getToAsid());
        when(sendAcknowledgementTaskDefinition.getTypeCode()).thenReturn(POSITIVE_ACK_TYPE_CODE);
        when(sendAcknowledgementTaskDefinition.getEhrRequestMessageId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendAcknowledgementTaskDefinition.getConversationId()).thenReturn(ehrExtractStatus.getConversationId());
        when(sendAcknowledgementTaskDefinition.getFromOdsCode()).thenReturn(ehrRequest.getFromOdsCode());
        when(mhsClient.sendMessageToMHS(request)).thenReturn("Successful Mhs Outbound Request");

        sendAcknowledgementExecutor.execute(sendAcknowledgementTaskDefinition);
        var ehrExtractFirst = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        sendAcknowledgementExecutor.execute(sendAcknowledgementTaskDefinition);
        var ehrExtractSecond = ehrExtractStatusRepository.findByConversationId(ehrExtractStatus.getConversationId()).get();

        assertThat(ehrExtractFirst.getUpdatedAt()).isNotEqualTo(ehrExtractSecond.getUpdatedAt());
    }

    @Test
    public void When_NegativeAcknowledgementTaskExecuted_Expect_ValidRequestSentToMhs() {
        var taskDefinition =
            SendAcknowledgementTaskDefinition.builder()
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .fromOdsCode(FROM_ODS_CODE)
                .toOdsCode(TO_ODS_CODE)
                .ehrRequestMessageId(EHR_REQUEST_MESSAGE_ID)
                .reasonCode(REASON_CODE)
                .detail(REASON_MESSAGE)
                .conversationId(EhrStatusConstants.CONVERSATION_ID)
                .taskId(TASK_ID)
                .typeCode(NEGATIVE_ACK_TYPE_CODE)
                .build();

        sendAcknowledgementExecutor.execute(taskDefinition);

        verify(mhsRequestBuilder).buildSendAcknowledgement(asString(expectedNackMessage), FROM_ODS_CODE, EhrStatusConstants.CONVERSATION_ID,
            GENERATED_RANDOM_ID);

        EhrExtractStatus updatedEhrExtractStatus =
            ehrExtractStatusRepository.findByConversationId(EhrStatusConstants.CONVERSATION_ID).get();
        EhrExtractStatus.AckToRequester ackToRequester = updatedEhrExtractStatus.getAckToRequester();
        assertThat(ackToRequester.getTaskId()).isEqualTo(TASK_ID);
        assertThat(ackToRequester.getMessageId()).isEqualTo(GENERATED_RANDOM_ID);
        assertThat(ackToRequester.getTypeCode()).isEqualTo(NEGATIVE_ACK_TYPE_CODE);
        assertThat(ackToRequester.getReasonCode()).isEqualTo(REASON_CODE);
        assertThat(ackToRequester.getDetail()).isEqualTo(REASON_MESSAGE);
    }

    @Test
    public void When_PositiveAcknowledgementTaskExecuted_Expect_ValidRequestSentToMhs() {
        var taskDefinition =
            SendAcknowledgementTaskDefinition.builder()
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .fromOdsCode(FROM_ODS_CODE)
                .toOdsCode(TO_ODS_CODE)
                .ehrRequestMessageId(EHR_REQUEST_MESSAGE_ID)
                .conversationId(EhrStatusConstants.CONVERSATION_ID)
                .taskId(TASK_ID)
                .typeCode(POSITIVE_ACK_TYPE_CODE)
                .build();

        sendAcknowledgementExecutor.execute(taskDefinition);

        verify(mhsRequestBuilder).buildSendAcknowledgement(asString(expectedAckMessage), FROM_ODS_CODE, EhrStatusConstants.CONVERSATION_ID,
            GENERATED_RANDOM_ID);

        EhrExtractStatus updatedEhrExtractStatus =
            ehrExtractStatusRepository.findByConversationId(EhrStatusConstants.CONVERSATION_ID).get();
        EhrExtractStatus.AckToRequester ackToRequester = updatedEhrExtractStatus.getAckToRequester();
        assertThat(ackToRequester.getTaskId()).isEqualTo(TASK_ID);
        assertThat(ackToRequester.getMessageId()).isEqualTo(GENERATED_RANDOM_ID);
        assertThat(ackToRequester.getTypeCode()).isEqualTo(POSITIVE_ACK_TYPE_CODE);
        assertThat(ackToRequester.getReasonCode()).isNull();
        assertThat(ackToRequester.getDetail()).isNull();
    }
}
