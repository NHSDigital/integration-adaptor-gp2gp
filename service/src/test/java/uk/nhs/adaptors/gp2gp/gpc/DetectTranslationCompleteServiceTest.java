package uk.nhs.adaptors.gp2gp.gpc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@ExtendWith(MockitoExtension.class)
public class DetectTranslationCompleteServiceTest {

    @Mock
    private SendEhrExtractCoreTaskDispatcher sendEhrExtractCoreTaskDispatcher;

    private DetectTranslationCompleteService detectTranslationCompleteService;

    @BeforeEach
    public void setUp() {
        detectTranslationCompleteService = new DetectTranslationCompleteService(sendEhrExtractCoreTaskDispatcher);
    }

    @Test
    public void When_AllPreparingDataStepsAreFinished_Expect_SendEhrExtractTaskCreated() {
        EhrExtractStatus ehrExtractStatus = buildEhrExtractStatus("object_name");

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);

        verify(sendEhrExtractCoreTaskDispatcher).send(ehrExtractStatus);
    }

    @Test
    public void When_AllPreparingDataStepsAreNotFinished_Expect_SendEhrExtractTaskNotCreated() {
        EhrExtractStatus ehrExtractStatus = buildEhrExtractStatus(null);

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);

        verify(sendEhrExtractCoreTaskDispatcher, never()).send(ehrExtractStatus);
    }

    private EhrExtractStatus buildEhrExtractStatus(String objectName) {
        EhrExtractStatus.GpcAccessStructured gpcAccessStructured = EhrExtractStatus.GpcAccessStructured.builder()
            .objectName(objectName)
            .build();

        EhrExtractStatus.GpcDocument document = EhrExtractStatus.GpcDocument.builder()
            .objectName(objectName)
            .build();

        EhrExtractStatus.GpcAccessDocument gpcAccessDocument = EhrExtractStatus.GpcAccessDocument.builder()
            .documents(Collections.singletonList(document))
            .build();

        return EhrExtractStatus.builder()
            .gpcAccessStructured(gpcAccessStructured)
            .gpcAccessDocument(gpcAccessDocument)
            .build();
    }
}
