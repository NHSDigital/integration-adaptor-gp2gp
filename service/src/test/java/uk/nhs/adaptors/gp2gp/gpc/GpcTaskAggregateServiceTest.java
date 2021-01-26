package uk.nhs.adaptors.gp2gp.gpc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCore;

@ExtendWith(MockitoExtension.class)
public class GpcTaskAggregateServiceTest {

    @Mock
    private SendEhrExtractCore sendEhrExtractCore;

    private GpcTaskAggregateService gpcTaskAggregateService;

    @BeforeEach
    public void setUp() {
        gpcTaskAggregateService = new GpcTaskAggregateService(sendEhrExtractCore);
    }

    @Test
    public void When_AllPreparingDataStepsAreFinished_Expect_SendEhrExtractTaskCreated() {
        EhrExtractStatus ehrExtractStatus = getEhrExtractStatus("object_name");

        gpcTaskAggregateService.sendData(ehrExtractStatus);

        verify(sendEhrExtractCore).send(ehrExtractStatus);
    }

    @Test
    public void When_AllPreparingDataStepsAreNotFinished_Expect_SendEhrExtractTaskNotCreated() {
        EhrExtractStatus ehrExtractStatus = getEhrExtractStatus(null);

        gpcTaskAggregateService.sendData(ehrExtractStatus);

        verify(sendEhrExtractCore, never()).send(ehrExtractStatus);
    }

    private EhrExtractStatus getEhrExtractStatus(String objectName) {
        EhrExtractStatus.GpcAccessStructured gpcAccessStructured = EhrExtractStatus.GpcAccessStructured.builder()
            .objectName(objectName)
            .build();

        EhrExtractStatus.GpcAccessDocument.GpcDocument document = EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
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
