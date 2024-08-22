package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import java.util.Optional;
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

import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObservationToNarrativeStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/observation/";
    private static final String NOPAT_CONFIDENTIALITY_CODE = ConfidentialityCodeUtility
        .getNopatHl7v3ConfidentialityCode();

    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = "example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = "example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = "example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = "example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_NO_DATES = "example-observation-resource-5.json";
    private static final String INPUT_JSON_WITH_PERFORMER = "example-observation-resource-25.json";
    private static final String INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE =
        "example-observation-with-performer-invalid-ref-resource-type.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_START = "example-observation-resource-38.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_NO_END = "example-observation-resource-39.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD_BLANK = "example-observation-resource-40.json";
    private static final String INPUT_JSON_WITH_SAMPLED_DATA_VALUE = "example-observation-with-sampleddata.json";
    private static final String INPUT_JSON_WITH_ATTACHMENT_VALUE = "example-observation-with-attachment.json";

    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = "expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_USES_ISSUED = "expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD_START = "expected-output-narrative-statement-3.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = "expected-output-narrative-statement-4.xml";
    private static final String OUTPUT_XML_USES_AGENT = "expected-output-narrative-statement-5.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private ConfidentialityService confidentialityService;

    private ObservationToNarrativeStatementMapper observationToNarrativeStatementMapper;
    private MessageContext messageContext;

    @BeforeEach
    void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(new Bundle());
        observationToNarrativeStatementMapper = new ObservationToNarrativeStatementMapper(messageContext, new ParticipantMapper(),
            confidentialityService);
    }

    @AfterEach
    void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        final String expectedXml = getXmlStringForFile(outputXml);
        final Observation parsedObservation = getObservationResourceFromJson(inputJson);

        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Practitioner, "something"));
        messageContext.getAgentDirectory().getAgentId(buildReference(ResourceType.Organization, "something"));

        final String actualXml = observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingObservationJsonWithNestedTrue_Expect_NarrativeStatementXmlOutput() {
        final String expectedXml = getXmlStringForFile(OUTPUT_XML_USES_NESTED_COMPONENT);
        final Observation observation = getObservationResourceFromJson(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);

        final String actualXml = observationToNarrativeStatementMapper
            .mapObservationToNarrativeStatement(observation, true);

        assertThat(actualXml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void When_MappingParsedObservationJsonWithNoDates_Expect_MapperException() {
        final Observation observation = getObservationResourceFromJson(INPUT_JSON_WITH_NO_DATES);

        assertThrows(EhrMapperException.class, () ->
            observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(observation, true));
    }

    @Test
    void When_MappingObservationWithInvalidParticipantResourceType_Expect_MapperException() {
        final Observation observation = getObservationResourceFromJson(INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE);
        final String expectedExceptionMessage = "Not supported agent reference: Patient/something";

        assertThatThrownBy(() -> observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(observation, true))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage(expectedExceptionMessage);
    }

    @Test
    void When_MappingObservation_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinNarrativeStatement() {
        final Observation observation = getObservationResourceFromJson(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.of(NOPAT_CONFIDENTIALITY_CODE));

        final String actualXml = observationToNarrativeStatementMapper
            .mapObservationToNarrativeStatement(observation, false);

        assertThat(actualXml).contains(NOPAT_CONFIDENTIALITY_CODE);
    }

    @Test
    void When_MappingObservation_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeNotToBePresent() {
        final Observation observation = getObservationResourceFromJson(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(observation);
        when(confidentialityService.generateConfidentialityCode(observation))
            .thenReturn(Optional.empty());

        final String actualXml = observationToNarrativeStatementMapper
            .mapObservationToNarrativeStatement(observation, false);

        assertThat(actualXml).doesNotContain(NOPAT_CONFIDENTIALITY_CODE);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsThrowError")
    void When_MappingObservationWithAttachmentAndSampleData_Expect_MapperException(String inputJson, Class<?> expectedClass) {
        final Observation parsedObservation = getObservationResourceFromJson(inputJson);
        final String expectedExceptionMessage = String.format(
            "Observation value type %s not supported.", expectedClass);

        final EhrMapperException exception = assertThrows(EhrMapperException.class, () ->
            observationToNarrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true));

        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
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

    private static Stream<Arguments> resourceFileParamsThrowError() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_SAMPLED_DATA_VALUE, SampledData.class),
            Arguments.of(INPUT_JSON_WITH_ATTACHMENT_VALUE, Attachment.class)
        );
    }

    private String getXmlStringForFile(String filename) {
        final String filePath = TEST_FILES_DIRECTORY + filename;
        return ResourceTestFileUtils.getFileContent(filePath);
    }

    private Observation getObservationResourceFromJson(String filename) {
        final String filePath = TEST_FILES_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Observation.class);
    }
}