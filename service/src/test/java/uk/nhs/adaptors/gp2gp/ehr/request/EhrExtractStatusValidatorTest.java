package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

public class EhrExtractStatusValidatorTest {

    private static final String OBJECT_NAME = "some-file-name";
    private static final String PATIENT_ID = "3";

    @Test
    public void When_AllPreparingDataStepsAreFinished_Expect_ReturnTrue() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isTrue();
    }

    private EhrExtractStatus.GpcDocument getFinishedGpcDocument() {
        return getGpcDocument(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcAccessStructured getFinishedGpcAccessStructured() {
        return getGpcAccessStructured(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcDocument getGpcDocument(String objectName) {
        return EhrExtractStatus.GpcDocument.builder()
            .objectName(objectName)
            .build();
    }

    private EhrExtractStatus.GpcAccessStructured getGpcAccessStructured(String objectName) {
        return EhrExtractStatus.GpcAccessStructured.builder()
            .objectName(objectName)
            .build();
    }

    @Test
    public void When_AllPreparingDataStepsAreFinishedAndDocumentsListIsEmpty_Expect_ReturnTrue() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Collections.emptyList(), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isTrue();
    }

    @Test
    public void When_AllPreparingDataStepsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getUnfinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getUnfinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    private EhrExtractStatus.GpcDocument getUnfinishedGpcDocument() {
        return getGpcDocument(null);
    }

    private EhrExtractStatus.GpcAccessStructured getUnfinishedGpcAccessStructured() {
        return getGpcAccessStructured(null);
    }

    @Test
    public void When_DocumentAccessStepIsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getUnfinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_OnlyOneDocumentAccessStepIsFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getFinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AccessStructuredStepIsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getUnfinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AllPreparingDataStepsWereNotStarted_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AccessStructuredStepIsNotStarted_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument()), PATIENT_ID
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AccessDocumentStepIsNotStarted_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AllDocumentsInEhrExtractStatusAreSent_Expect_True() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();
        List<EhrExtractStatus.GpcDocument> documentList = new ArrayList<>();
        documentList.add(getFinishedGpcDocumentSent());
        ehrExtractStatus.setGpcAccessDocument(new EhrExtractStatus.GpcAccessDocument(documentList, "123"));
        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus)).isTrue();
    }

    private EhrExtractStatus.GpcDocument getFinishedGpcDocumentSent() {
        var doc = getFinishedGpcDocument();
        doc.setSentToMhs(new EhrExtractStatus.GpcAccessDocument.SentToMhs(List.of("123"), "123", "123"));
        return doc;
    }

    @Test
    public void When_OneDocumentIsSentAndOneDocumentNotSent_Expect_False() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();
        List<EhrExtractStatus.GpcDocument> documentList = new ArrayList<>();
        documentList.add(getFinishedGpcDocumentSent());
        documentList.add(getFinishedGpcDocument());
        ehrExtractStatus.setGpcAccessDocument(new EhrExtractStatus.GpcAccessDocument(documentList, "123"));
        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_NoDocumentsInEhrExtractStatus_Expect_False() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();
        List<EhrExtractStatus.GpcDocument> documentList = new ArrayList<>();
        documentList.add(getFinishedGpcDocument());
        ehrExtractStatus.setGpcAccessDocument(new EhrExtractStatus.GpcAccessDocument(documentList, "123"));
        assertThat(EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus)).isFalse();
    }
}
