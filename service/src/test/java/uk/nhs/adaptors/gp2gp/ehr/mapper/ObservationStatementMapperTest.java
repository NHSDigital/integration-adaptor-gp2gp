package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Immunization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ObservationStatementMapperTest {

    private static final String TEST_ID = "C93659E1-1107-441C-BE25-C5EF4B7831D1";
    private static final String INPUT_JSON_WITH_PERTINENT_INFORMATION = "/ehr/mapper/immunization/immunization_all_pertinent_information.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private ObservationStatementMapper observationStatementMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        observationStatementMapper = new ObservationStatementMapper(randomIdGeneratorService);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectiveDatetime_Expect_NarrativeStatementXmlOutput() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERTINENT_INFORMATION);
        Immunization parsedImmunization = new FhirParseService().parseResource(jsonInput, Immunization.class);

        String outputMessage = observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, false);
    }
}
