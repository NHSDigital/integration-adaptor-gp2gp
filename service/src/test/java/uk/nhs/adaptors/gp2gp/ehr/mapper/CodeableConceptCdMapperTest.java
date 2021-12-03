package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

public class CodeableConceptCdMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/codeableconcept/";
    private static final String TEST_FILE_DIRECTORY_NULL_FLAVOR = "/ehr/mapper/codeableconcept/nullFlavor/";
    private static final String TEST_FILE_DIRECTORY_ACTUAL_PROBLEM = "/ehr/mapper/codeableconcept/actualProblem/";
    private static final String TEST_FILE_DIRECTORY_ALLERGY_CLINICAL_STATUS = "/ehr/mapper/codeableconcept/allergyClinicalStatus/";

    private FhirParseService fhirParseService;
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private static Stream<Arguments> getTestArguments() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY);
    }

    private static Stream<Arguments> getTestArgumentsNullFlavor() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_NULL_FLAVOR);
    }

    private static Stream<Arguments> getTestArgumentsActualProblem() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_ACTUAL_PROBLEM);
    }

    private static Stream<Arguments> getTestArgumentsAllergyClinicalStatus() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_ALLERGY_CLINICAL_STATUS);
    }

    @BeforeEach
    public void setUp() {
        fhirParseService = new FhirParseService();
        codeableConceptCdMapper = new CodeableConceptCdMapper();
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    public void When_MappingStubbedCodeableConcept_Expect_HL7CdObjectXml(String inputJson, String outputXml) throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCd(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsActualProblem")
    public void When_MappingStubbedCodeableConceptForActualProblemHeader_Expect_HL7CdObjectXml(String inputJson, String outputXml)
            throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(codeableConcept);
        assertThat(outputMessage)
                .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
                .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsNullFlavor")
    public void When_MappingStubbedCodeableConceptAsNullFlavor_Expect_HL7CdObjectXml(String inputJson, String outputXml)
        throws IOException {
        var observationCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(observationCodeableConcept, Observation.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapToNullFlavorCodeableConcept(codeableConcept);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("getTestArgumentsAllergyClinicalStatus")
    public void When_MappingStubbedCodeableConceptAsAllergy_Expect_HL7CdObjectXml(String inputJson, String outputXml)
        throws IOException {
        var allergyCodeableConcept = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var codeableConcept = fhirParseService.parseResource(allergyCodeableConcept, AllergyIntolerance.class).getCode();

        var outputMessage = codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(codeableConcept,
            AllergyIntolerance.AllergyIntoleranceClinicalStatus.RESOLVED);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }
}
