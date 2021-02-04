package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

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

    private EhrExtractStatus.GpcAccessStructured getFinishedGpcAccessStructured() {
        return getGpcAccessStructured(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcAccessStructured getUnfinishedGpcAccessStructured() {
        return getGpcAccessStructured(null);
    }

    private EhrExtractStatus.GpcAccessStructured getGpcAccessStructured(String objectName) {
        return EhrExtractStatus.GpcAccessStructured.builder()
            .objectName(objectName)
            .build();
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getUnfinishedGpcDocument() {
        return getGpcDocument(null);
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getFinishedGpcDocument() {
        return getGpcDocument(OBJECT_NAME);
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getGpcDocument(String objectName) {
        return EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
            .objectName(objectName)
            .build();
    }
}
