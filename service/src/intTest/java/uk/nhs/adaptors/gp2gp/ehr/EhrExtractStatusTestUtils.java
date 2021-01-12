package uk.nhs.adaptors.gp2gp.ehr;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class EhrExtractStatusTestUtils {
    public static EhrExtractStatus prepareEhrExtractStatus() {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return new EhrExtractStatus(
            now,
            now,
            EhrStatusConstants.CONVERSATION_ID,
            new EhrExtractStatus.EhrRequest(EhrStatusConstants.REQUEST_ID,
                EhrStatusConstants.NHS_NUMBER,
                EhrStatusConstants.FROM_PARTY_ID,
                EhrStatusConstants.TO_PARTY_ID,
                EhrStatusConstants.FROM_ASID,
                EhrStatusConstants.TO_ASID,
                EhrStatusConstants.FROM_ODS_CODE,
                EhrStatusConstants.TO_ODS_CODE)
        );
    }
}
