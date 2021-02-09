package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
public class NarrativeStatementMapperTest {
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private static final String TEST_ID = "394559384658936";
    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = "/ehr/mapper/observation/example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = "/ehr/mapper/observation/example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = "/ehr/mapper/observation/example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = "/ehr/mapper/observation/example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_NO_DATES = "/ehr/mapper/observation/example-observation-resource-5.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = "/ehr/mapper/observation/expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_USES_ISSUED = "/ehr/mapper/observation/expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD_START = "/ehr/mapper/observation/"
        + "expected-output-narrative-statement-3.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = "/ehr/mapper/observation/"
        + "expected-output-narrative-statement-4.xml";

    private CharSequence expectedOutputMessage;
    private NarrativeStatementMapper narrativeStatementMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        MessageContext.setMessageContext(new IdMapper(randomIdGeneratorService));
        narrativeStatementMapper = new NarrativeStatementMapper();
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @AfterEach
    public void tearDown() {
        MessageContext.resetMessageContext();
        TimeZone.setDefault(null);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectiveDatetime_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_EFFECTIVE_DATE_TIME);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNullEffectiveDatetime_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_ISSUED);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithEffectivePeriodStart_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_EFFECTIVE_PERIOD_START);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_PERIOD);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithoutEffective_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_ISSUED);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_ISSUED_ONLY);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNestedTrue_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNoDates_Expect_MapperException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_DATES);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThrows(EhrMapperException.class, ()
            -> narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true));
    }
}
