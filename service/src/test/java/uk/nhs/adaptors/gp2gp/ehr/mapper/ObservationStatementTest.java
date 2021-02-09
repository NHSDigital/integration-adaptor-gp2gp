package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObservationStatementTest {
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private static final String TEST_ID = "394559384658936";
    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = "/ehr/mapper/uncategorised/example-uncategorised-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_DATE_TIME = "/ehr/mapper/uncategorised/example-uncategorised-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = "/ehr/mapper/uncategorised/example-uncategorised-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = "/ehr/mapper/uncategorised/example-uncategorised-observation-resource-4.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = "/ehr/mapper/uncategorised/expected-output-observation-statement-1.xml";
    private static final String OUTPUT_XML_USES_UNK_DATE_TIME = "/ehr/mapper/uncategorised/expected-output-observation-statement-2.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = "/ehr/mapper/uncategorised/expected-output-observation-statement-3.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD = "/ehr/mapper/uncategorised/expected-output-observation-statement-4.xml";


    private CharSequence expectedOutputMessage;
    private ObservationStatementMapper observationStatementMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        observationStatementMapper = new ObservationStatementMapper(randomIdGeneratorService);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @AfterAll
    public static void tearDown() {
        TimeZone.setDefault(null);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectiveDatetime_Expect_ObservationStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_EFFECTIVE_DATE_TIME);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonNullEffectiveDatetime_Expect_ObservationStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_UNK_DATE_TIME);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NULL_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithoutEffective_Expect_ObservationStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_UNK_DATE_TIME);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_ISSUED_ONLY);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNestedTrue_Expect_ObservationStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectivePeriod_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_EFFECTIVE_PERIOD);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_PERIOD);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
