package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.CONVERSATION_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_CONTENT_TYPE;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_ID;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.DOCUMENT_NAME;
import static uk.nhs.adaptors.gp2gp.ehr.EhrStatusConstants.GPC_ACCESS_DOCUMENT_URL;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

public class EhrExtractStatusTestUtils {
    public static EhrExtractStatus prepareEhrExtractStatus() {
        return prepareEhrExtractStatus(CONVERSATION_ID);
    }

    public static EhrExtractStatus prepareEhrExtractStatus(String conversationId) {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(conversationId)
            .ehrRequest(prepareEhrRequest())
            .gpcAccessDocument(prepareGpcAccessDocument(DOCUMENT_ID))
            .build();
    }

    public static EhrExtractStatus prepareEhrExtractStatusNoDocuments(String conversationId) {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(conversationId)
            .gpcAccessDocument(prepareEmptyGpcAccessDocument())
            .build();
    }

    public static EhrExtractStatus prepareEhrExtractStatus(String conversationId, String documentId) {
        Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        return EhrExtractStatus.builder()
            .created(now)
            .updatedAt(now)
            .conversationId(conversationId)
            .ehrRequest(prepareEhrRequest())
            .gpcAccessDocument(prepareGpcAccessDocument(documentId))
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
            EhrStatusConstants.TO_ODS_CODE,
            EhrStatusConstants.MESSAGE_ID);
    }

    private static EhrExtractStatus.GpcAccessDocument prepareGpcAccessDocument(String documentId) {
        return EhrExtractStatus.GpcAccessDocument.builder()
            .documents(List.of(
                EhrExtractStatus.GpcDocument.builder()
                    .messageId(CONVERSATION_ID)
                    .documentId(documentId)
                    .objectName(DOCUMENT_NAME)
                    .contentType(DOCUMENT_CONTENT_TYPE)
                    .accessDocumentUrl(String.format(GPC_ACCESS_DOCUMENT_URL, documentId))
                    .build()
            )).build();
    }

    private static EhrExtractStatus.GpcAccessDocument prepareEmptyGpcAccessDocument() {
        return EhrExtractStatus.GpcAccessDocument.builder()
            .documents(new ArrayList<>())
            .build();
    }
}
