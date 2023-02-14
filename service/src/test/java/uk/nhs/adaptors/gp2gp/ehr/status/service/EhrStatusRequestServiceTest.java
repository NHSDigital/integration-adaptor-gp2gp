package uk.nhs.adaptors.gp2gp.ehr.status.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

@ExtendWith(MockitoExtension.class)
public class EhrStatusRequestServiceTest {

    private static final String TO_ASID_CODE = "test-to-asid";
    private static final String FROM_ASID_CODE = "test-from-asid";
    private static final String TO_ODS_CODE = "test-to-ods";
    private static final String FROM_ODS_CODE = "test-from-ods";
    private static final String NHS_NUMBER = "test-nhs-number";
    private static final String CONVERSATION_ID = "test-conversation-id";
    private static final Instant CREATED_DATE = LocalDate.parse("2016-04-17").atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Instant UPDATED_DATE = LocalDate.parse("2022-04-17").atStartOfDay().toInstant(ZoneOffset.UTC);

    private static final EhrExtractStatus EHR_EXTRACT_FOUND_COMPLETED_RECORD = EhrExtractStatus.builder()
        .created(CREATED_DATE)
        .updatedAt(UPDATED_DATE)
        .conversationId(CONVERSATION_ID)
        .ackPending(EhrExtractStatus.AckPending.builder().typeCode("AA").build())
        .ackToRequester(EhrExtractStatus.AckToRequester.builder().typeCode("AA").build())
        .ehrReceivedAcknowledgement(EhrExtractStatus.EhrReceivedAcknowledgement.builder().conversationClosed(Instant.now()).build())
        .ehrRequest(EhrExtractStatus.EhrRequest.builder()
            .toAsid(TO_ASID_CODE)
            .fromAsid(FROM_ASID_CODE)
            .toOdsCode(TO_ODS_CODE)
            .fromOdsCode(FROM_ODS_CODE)
            .nhsNumber(NHS_NUMBER)
            .build())
        .build();

    @Mock
    private MongoTemplate mongoTemplate;
    @InjectMocks
    private EhrStatusRequestsService ehrStatusRequestsService;

    @Test
    public void When_GetEhrStatusRequests_WithNoFoundRecordsInMongoDb_Expect_EmptyArrayListResponse() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(null);

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests).isEqualTo(Optional.empty());
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_SingleResponse() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().size()).isEqualTo(1);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsInitialRequestTimestampCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getInitialRequestTimestamp()).isEqualTo(CREATED_DATE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsActionCompletedTimestampCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getActionCompletedTimestamp()).isEqualTo(UPDATED_DATE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsActionConversationIdCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getConversationId()).isEqualTo(CONVERSATION_ID);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsActionNHSNumberCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getNhsNumber()).isEqualTo(NHS_NUMBER);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsActionMigrationStatusCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsFromASIDCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getFromAsid()).isEqualTo(FROM_ASID_CODE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsToASIDCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getToAsid()).isEqualTo(TO_ASID_CODE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsFromODSCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getFromOdsCode()).isEqualTo(FROM_ODS_CODE);
    }

    @Test
    public void When_GetEhrStatusRequests_WithOneFoundRecordInMongoDb_Expect_ResponseMapsToOdsCorrectly() {

        var query = new EhrStatusRequestQuery();
        when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(EHR_EXTRACT_FOUND_COMPLETED_RECORD));

        Optional<List<EhrStatusRequest>> statusRequests = ehrStatusRequestsService.getEhrStatusRequests(query);

        assertThat(statusRequests.get().get(0).getToOdsCode()).isEqualTo(TO_ODS_CODE);
    }

}
