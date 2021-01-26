package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import uk.nhs.adaptors.gp2gp.IdGenerator;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

public class EhrExtractStatusValidatorTest {

    private static final String TASK_ID = IdGenerator.get();
    private static final String OBJECT_NAME = "some-file-name";

    @Test
    public void When_AllPreparingDataStepsAreFinished_Expect_ReturnTrue() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument())
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isTrue();
    }

    @Test
    public void When_AllPreparingDataStepsAreFinishedAndDocumentsListIsEmpty_Expect_ReturnTrue() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Collections.emptyList()
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isTrue();
    }

    @Test
    public void When_AllPreparingDataStepsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getUnfinishedGpcDocument())
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getUnfinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_DocumentAccessStepIsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getUnfinishedGpcDocument())
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_OnlyOneDocumentAccessStepIsFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getUnfinishedGpcDocument(), getFinishedGpcDocument())
        );
        ehrExtractStatus.setGpcAccessDocument(gpcAccessDocument);
        ehrExtractStatus.setGpcAccessStructured(getFinishedGpcAccessStructured());

        assertThat(EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)).isFalse();
    }

    @Test
    public void When_AccessStructuredStepIsNotFinished_Expect_ReturnFalse() {
        EhrExtractStatus ehrExtractStatus = new EhrExtractStatus();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = new EhrExtractStatus.GpcAccessDocument(
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument())
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
            Arrays.asList(getFinishedGpcDocument(), getFinishedGpcDocument())
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
        return getGpcAccessStructured(OBJECT_NAME + IdGenerator.get());
    }

    private EhrExtractStatus.GpcAccessStructured getUnfinishedGpcAccessStructured() {
        return getGpcAccessStructured(null);
    }

    private EhrExtractStatus.GpcAccessStructured getGpcAccessStructured(String objectName) {
        return new EhrExtractStatus.GpcAccessStructured(
            objectName,
            Instant.now(),
            TASK_ID
        );
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getUnfinishedGpcDocument() {
        return getGpcDocument(null);
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getFinishedGpcDocument() {
        return getGpcDocument(OBJECT_NAME + IdGenerator.get());
    }

    private EhrExtractStatus.GpcAccessDocument.GpcDocument getGpcDocument(String objectName) {
        return new EhrExtractStatus.GpcAccessDocument.GpcDocument(
            IdGenerator.get(),
            IdGenerator.get(),
            objectName,
            Instant.now(),
            TASK_ID
        );
    }
}
