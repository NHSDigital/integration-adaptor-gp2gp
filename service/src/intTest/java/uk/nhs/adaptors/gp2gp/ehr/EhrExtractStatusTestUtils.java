package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.GPC_ACCESS_DOCUMENT_URL;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

public class EhrExtractStatusTestUtils {
    public static EhrExtractStatus prepareEhrExtractStatus() {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(EhrStatusConstants.CONVERSATION_ID)
            .ehrRequest(prepareEhrRequest())
            .gpcAccessDocument(prepareGpcAccessDocument())
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

    private static EhrExtractStatus.GpcAccessDocument prepareGpcAccessDocument() {
        return EhrExtractStatus.GpcAccessDocument.builder()
            .documents(List.of(
                EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
                    .documentId(DOCUMENT_ID)
                    .accessDocumentUrl(GPC_ACCESS_DOCUMENT_URL)
                    .build()
            )).build();
    }
}
