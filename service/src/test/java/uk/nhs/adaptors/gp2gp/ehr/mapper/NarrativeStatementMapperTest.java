package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class NarrativeStatementMapperTest {
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private static final String TEST_ID = "394559384658936";
    private static final String INPUT_JSON_1 = "/ehr/mapper/observation/example-observation-resource-1.json";
    private static final String INPUT_JSON_2 = "/ehr/mapper/observation/example-observation-resource-2.json";
    private static final String INPUT_JSON_3 = "/ehr/mapper/observation/example-observation-resource-3.json";
    private static final String INPUT_JSON_4 = "/ehr/mapper/observation/example-observation-resource-4.json";
    private static final String OUTPUT_XML_1 = "/ehr/mapper/observation/expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_2 = "/ehr/mapper/observation/expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_3 = "/ehr/mapper/observation/expected-output-narrative-statement-3.xml";

    private CharSequence expectedOutputMessage;
    private NarrativeStatementMapper narrativeStatementMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        narrativeStatementMapper = new NarrativeStatementMapper(randomIdGeneratorService);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectiveDatetime_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_1);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_1);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNullEffectiveDatetime_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_2);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_2);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectivePeriod_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_3);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_3);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithoutEffective_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_2);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_4);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
