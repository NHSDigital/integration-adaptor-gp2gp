package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Immunization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImmunizationObservationStatementMapperTest {

    private static final String TEST_ID = "C93659E1-1107-441C-BE25-C5EF4B7831D1";
    private static final String INPUT_JSON_WITH_PERTINENT_INFORMATION =
        "/ehr/mapper/immunization/immunization-all-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_PERTINENT_INFORMATION =
        "/ehr/mapper/immunization/immunization-no-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_DATE_RECORDED_EXTENSION =
        "/ehr/mapper/immunization/immunization-no-date-recorded.json";
    private static final String INPUT_JSON_BUNDLE = "/ehr/mapper/immunization/fhir-bundle.json";
    private static final String OUTPUT_XML_WITH_PERTINENT_INFORMATION =
        "/ehr/mapper/immunization/expected-output-observation-statement-all-information.xml";
    private static final String OUTPUT_XML_WITHOUT_PERTINENT_INFORMATION =
        "/ehr/mapper/immunization/expected-output-observation-statement-no-information.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private ImmunizationObservationStatementMapper observationStatementMapper;
    private Bundle bundle;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() throws IOException {
        fhirParseService = new FhirParseService();
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        observationStatementMapper = new ImmunizationObservationStatementMapper(randomIdGeneratorService);
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = fhirParseService.parseResource(bundleInput, Bundle.class);
    }

    @Test
    public void When_MappingParsedImmunizationJsonWithPertinentInformation_Expect_NarrativeStatementXmlOutput() throws IOException {
        var expectedOutput = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_PERTINENT_INFORMATION);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERTINENT_INFORMATION);
        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        String outputMessage = observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, bundle, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    public void When_MappingParsedImmunizationJsonWithoutPertinentInformation_Expect_NarrativeStatementXmlOutput() throws IOException {
        var expectedOutput = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITHOUT_PERTINENT_INFORMATION);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITHOUT_PERTINENT_INFORMATION);
        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        String outputMessage = observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, bundle, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    public void When_MappingParsedImmunizationJsonWithoutDateRecordedExtension_Expect_Error() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITHOUT_DATE_RECORDED_EXTENSION);
        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);

        assertThrows(EhrMapperException.class, ()
            -> observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, bundle, false));
    }
}
