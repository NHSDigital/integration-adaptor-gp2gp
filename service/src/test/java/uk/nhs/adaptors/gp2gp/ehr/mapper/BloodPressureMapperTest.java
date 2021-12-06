package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BloodPressureMapperTest {
    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String BLOOD_PRESSURE_FILE_LOCATION = "/ehr/mapper/blood_pressure/";

    private static final String INPUT_ARTERIAL_PRESSURE_WITH_DATA = "arterial-pressure-with-data.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITH_DATA = "arterial-pressure-with-data.xml";
    private static final String INPUT_ARTERIAL_PRESSURE_WITH_PERIOD_DATE = "arterial-pressure-with-period-date.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITH_PERIOD_DATE = "arterial-pressure-with-period-date.xml";
    private static final String INPUT_ARTERIAL_PRESSURE_WITHOUT_DATA = "arterial-pressure-without-data.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITHOUT_DATA = "arterial-pressure-without-data.xml";
    private static final String INPUT_BLOOD_PRESSURE_WITH_DATA = "blood-pressure-with-data.json";
    private static final String EXPECTED_BLOOD_PRESSURE_WITH_DATA = "blood-pressure-with-data.xml";
    private static final String EXPECTED_NESTED_BLOOD_PRESSURE = "blood-pressure-with-data-nested.xml";
    private static final String INPUT_BLOOD_PRESSURE_WITHOUT_DATA = "blood-pressure-without-data.json";
    private static final String EXPECTED_BLOOD_PRESSURE_WITHOUT_DATA = "blood-pressure-without-data.xml";
    private static final String INPUT_EMPTY_OBSERVATION = "empty-observation.json";
    private static final String EXPECTED_EMPTY_OBSERVATION = "empty-observation.xml";
    private static final String INPUT_OBSERVATION_WITHOUT_VALID_CODE = "observation-with-no-valid-code.json";
    private static final String EXPECTED_OBSERVATION_WITHOUT_VALID_CODE = "observation-with-no-valid-code.xml";
    private static final String INPUT_ARTERIAL_PRESSURE_WITH_SYSTOLIC_DATA_ONLY = "arterial-pressure-with-systolic-data-only.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITH_SYSTOLIC_DATA_ONLY = "arterial-pressure-with-systolic-data-only.xml";
    private static final String INPUT_ARTERIAL_PRESSURE_WITH_DIASTOLIC_DATA_ONLY = "arterial-pressure-with-diastolic-data-only.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITH_DIASTOLIC_DATA_ONLY = "arterial-pressure-with-diastolic-data-only.xml";
    private static final String INPUT_ARTERIAL_PRESSURE_WITHOUT_EFFECTIVE_DATE = "arterial-pressure-without-effective-date.json";
    private static final String EXPECTED_ARTERIAL_PRESSURE_WITHOUT_EFFECTIVE_DATE = "arterial-pressure-without-effective-date.xml";
    private static final String INPUT_BLOOD_PRESSURE_WITH_CODEABLE_CONCEPTS = "blood-pressure-with-codeable-concepts.json";
    private static final String EXPECTED_BLOOD_PRESSURE_WITH_CODEABLE_CONCEPTS = "blood-pressure-with-codeable-concepts.xml";
    private static final String INPUT_BLOOD_PRESSURE_WITH_NO_CODEABLE_CONCEPTS = "blood-pressure-with-no-codeable-concepts.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper mockCodeableConceptCdMapper;

    private MessageContext messageContext;
    private BloodPressureMapper bloodPressureMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(new Bundle());
        bloodPressureMapper = new BloodPressureMapper(
            messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
            mockCodeableConceptCdMapper, new ParticipantMapper());
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingEmptyObservation_Expect_CompoundStatementXmlReturned() throws IOException {
        when(mockCodeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        var jsonInput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + INPUT_EMPTY_OBSERVATION);
        var expectedOutput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + EXPECTED_EMPTY_OBSERVATION);

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);
        var outputMessage = bloodPressureMapper.mapBloodPressure(observation, false);

        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingBloodPressureWithNestedTrue_Expect_CompoundStatementXmlReturned() throws IOException {
        when(mockCodeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        var jsonInput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + INPUT_BLOOD_PRESSURE_WITH_DATA);
        var expectedOutput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + EXPECTED_NESTED_BLOOD_PRESSURE);

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);
        var outputMessage = bloodPressureMapper.mapBloodPressure(observation, true);

        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void When_MappingBloodPressure_Expect_CompoundStatementXmlReturned(String inputJson, String outputXml) throws IOException {
        when(mockCodeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        var jsonInput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + outputXml);

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);
        var outputMessage = bloodPressureMapper.mapBloodPressure(observation, false);

        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITH_DATA, EXPECTED_ARTERIAL_PRESSURE_WITH_DATA),
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITH_PERIOD_DATE, EXPECTED_ARTERIAL_PRESSURE_WITH_PERIOD_DATE),
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITHOUT_DATA, EXPECTED_ARTERIAL_PRESSURE_WITHOUT_DATA),
            Arguments.of(INPUT_BLOOD_PRESSURE_WITH_DATA, EXPECTED_BLOOD_PRESSURE_WITH_DATA),
            Arguments.of(INPUT_BLOOD_PRESSURE_WITHOUT_DATA, EXPECTED_BLOOD_PRESSURE_WITHOUT_DATA),
            Arguments.of(INPUT_OBSERVATION_WITHOUT_VALID_CODE, EXPECTED_OBSERVATION_WITHOUT_VALID_CODE),
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITH_SYSTOLIC_DATA_ONLY, EXPECTED_ARTERIAL_PRESSURE_WITH_SYSTOLIC_DATA_ONLY),
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITH_DIASTOLIC_DATA_ONLY, EXPECTED_ARTERIAL_PRESSURE_WITH_DIASTOLIC_DATA_ONLY),
            Arguments.of(INPUT_ARTERIAL_PRESSURE_WITHOUT_EFFECTIVE_DATE, EXPECTED_ARTERIAL_PRESSURE_WITHOUT_EFFECTIVE_DATE)
        );
    }

    @Test
    public void When_MappingBloodPressureWithCodeableConcepts_Expect_CompoundStatementXmlReturned() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + INPUT_BLOOD_PRESSURE_WITH_CODEABLE_CONCEPTS);
        var expectedOutput = ResourceTestFileUtils.getFileContent(
            BLOOD_PRESSURE_FILE_LOCATION + EXPECTED_BLOOD_PRESSURE_WITH_CODEABLE_CONCEPTS);

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        bloodPressureMapper = new BloodPressureMapper(
            messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
            codeableConceptCdMapper, new ParticipantMapper());

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);
        var outputMessage = bloodPressureMapper.mapBloodPressure(observation, true);

        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingBloodPressureWithNoCodeableConcepts_Expect_Exception() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(BLOOD_PRESSURE_FILE_LOCATION + INPUT_BLOOD_PRESSURE_WITH_NO_CODEABLE_CONCEPTS);

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        bloodPressureMapper = new BloodPressureMapper(
            messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
            codeableConceptCdMapper, new ParticipantMapper());

        Observation observation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThrows(EhrMapperException.class, ()
            -> bloodPressureMapper.mapBloodPressure(observation, true));
    }
}
