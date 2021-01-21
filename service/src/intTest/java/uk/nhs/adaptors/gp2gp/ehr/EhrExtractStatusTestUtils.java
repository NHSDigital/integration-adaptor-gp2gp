package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class EhrExtractStatusTestUtils {
    public static EhrExtractStatus prepareEhrExtractStatus() {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(EhrStatusConstants.CONVERSATION_ID)
            .ehrRequest(prepareEhrRequest())
            .gpcAccessDocuments(prepareGpcAccessDocuments())
            .build();
    }

    private static EhrExtractStatus.EhrRequest prepareEhrRequest() {
        return new EhrExtractStatus.EhrRequest(EhrStatusConstants.REQUEST_ID,
            EhrStatusConstants.NHS_NUMBER,
            EhrStatusConstants.FROM_PARTY_ID,
            EhrStatusConstants.TO_PARTY_ID,
            EhrStatusConstants.FROM_ASID,
            EhrStatusConstants.TO_ASID,
            EhrStatusConstants.FROM_ODS_CODE,
            EhrStatusConstants.TO_ODS_CODE);
    }

    private static List<EhrExtractStatus.GpcAccessDocument> prepareGpcAccessDocuments() {
        return ImmutableList.of(EhrExtractStatus.GpcAccessDocument.builder()
            .objectName(EhrStatusConstants.DOCUMENT_ID + ".json")
            .build()
        );
    }
}
