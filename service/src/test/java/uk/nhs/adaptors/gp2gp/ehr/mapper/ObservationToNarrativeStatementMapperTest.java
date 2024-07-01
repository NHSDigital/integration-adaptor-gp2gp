package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ObservationToNarrativeStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/observation/";
    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY + "example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = TEST_FILE_DIRECTORY + "example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-observation-resource-5.json";
    private static final String INPUT_JSON_WITH_PERFORMER = TEST_FILE_DIRECTORY + "example-observation-resource-25.json";
    private static final String INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE = TEST_FILE_DIRECTORY
        + "example-observation-with-performer-invalid-ref-resource-type.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_START = TEST_FILE_DIRECTORY + "example-observation-resource-38.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_END = TEST_FILE_DIRECTORY + "example-observation-resource-39.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_BLANK = TEST_FILE_DIRECTORY + "example-observation-resource-40.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_USES_ISSUED = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD_START = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-3.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-4.xml";
    private static final String OUTPUT_XML_USES_AGENT = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-5.xml";
    private static final String INPUT_JSON_WITH_SAMPLED_DATA_VALUE = TEST_FILE_DIRECTORY + "example-observation-with-sampleddata.json";
    private static final String INPUT_JSON_WITH_ATTACHMENT_VALUE = TEST_FILE_DIRECTORY + "example-observation-with-attachment.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CharSequence expectedOutputMessage;
    private ObservationToNarrativeStatementMapper observationToNarrativeStatementMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(new Bundle());
        observationToNarrativeStatementMapper = new ObservationToNarrativeStatementMapper(messageContext, new ParticipantMapper());
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Practitioner, "something"));
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Organization, "something"));

        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_EFFECTIVE_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_ISSUED),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD, OUTPUT_XML_USES_EFFECTIVE_PERIOD_START),
            Arguments.of(INPUT_JSON_WITH_ISSUED_ONLY, OUTPUT_XML_USES_ISSUED),
            Arguments.of(INPUT_JSON_WITH_PERFORMER, OUTPUT_XML_USES_AGENT),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_START, OUTPUT_XML_USES_ISSUED),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_END, OUTPUT_XML_USES_EFFECTIVE_PERIOD_START),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD_BLANK, OUTPUT_XML_USES_ISSUED)
        );
    }

    @Test
    public void When_MappingObservationJsonWithNestedTrue_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNoDates_Expect_MapperException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_DATES);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThrows(EhrMapperException.class, ()
            -> observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true));
    }

    @Test
    public void When_MappingObservationWithInvalidParticipantResourceType_Expect_MapperException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThatThrownBy(() -> observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Not supported agent reference: Patient/something");
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsThrowError")
    public void When_MappingObservationWithAttachmentAndSampleData_Expect_MapperException(String inputJson, Class expectedClass)
        throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        var exception = assertThrows(EhrMapperException.class, () ->
            observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true));

        assertThat(exception.getMessage()).isEqualTo(String.format(
            "Observation value type %s not supported.", expectedClass));
    }

    private static Stream<Arguments> resourceFileParamsThrowError() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_SAMPLED_DATA_VALUE, SampledData.class),
            Arguments.of(INPUT_JSON_WITH_ATTACHMENT_VALUE, Attachment.class)
        );
    }
}
