package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

public class CodeableConceptCdMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/codeableconcept/";
    private static final String TEST_FILE_DIRECTORY_NULL_FLAVOR = "/ehr/mapper/codeableconcept/nullFlavor/";
    private static final String TEST_FILE_DIRECTORY_ACTUAL_PROBLEM = "/ehr/mapper/codeableconcept/actualProblem/";
    private static final String TEST_FILE_DIRECTORY_ALLERGY_CLINICAL_STATUS = "/ehr/mapper/codeableconcept/allergyClinicalStatus/";
    private static final String TEST_FILE_TOPIC_RELATED_CONDITION = TEST_FILE_DIRECTORY
        + "topic/codeable_concept_snowmed_related_condtition.json";
    private static final String CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE = TEST_FILE_DIRECTORY
        + "topic/cd_for_topic_related_problem_and_title.xml";
    private static final String CD_FOR_TOPIC_TITLE = TEST_FILE_DIRECTORY + "topic/cd_for_topic_title.xml";
    private static final String CD_FOR_TOPIC_UNSPECIFIED = TEST_FILE_DIRECTORY + "topic/cd_for_topic_unspecified.xml";
    private static final String TEST_FILE_DIRECTORY_TOPIC_RELATED_PROBLEM = TEST_FILE_DIRECTORY + "topic/relatedProblem/";
    private static final String CD_FOR_CATEGORY_TITLE = TEST_FILE_DIRECTORY + "category/cd_for_category_titile.xml";
    private static final String CD_FOR_CATEGORY_NO_TITLE = TEST_FILE_DIRECTORY + "category/cd_for_category_no_title.xml";

    private static final String TEST_TITLE = "test title";

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

    private static Stream<Arguments> getTestArgumentsForTopicRelatedProblem() {
        return TestArgumentsLoaderUtil.readTestCases(TEST_FILE_DIRECTORY_TOPIC_RELATED_PROBLEM);
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

    @ParameterizedTest
    @MethodSource("getTestArgumentsForTopicRelatedProblem")
    @SneakyThrows
    public void When_MappingCdForTopic_With_RelatedProblem_Expect_HL7CdObjectXml(String inputJson, String outputXml) {
        var condition = ResourceTestFileUtils.getFileContent(inputJson);
        var codeableConcept = fhirParseService.parseResource(condition, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept);

        assertThat(outputString)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_With_RelatedProblemAndTitle_Expect_ProblemCodeAndTitleAreUsed() {
        var relatedProblem = ResourceTestFileUtils.getFileContent(TEST_FILE_TOPIC_RELATED_CONDITION);
        var codeableConcept = fhirParseService.parseResource(relatedProblem, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept, TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_With_TitleOnly_Expect_UnspecifiedProblemAndTitle() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(TEST_TITLE);

        assertThat(outputString)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForTopic_Without_RelatedProblemOrTile_Expect_UnspecifiedProblem() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_UNSPECIFIED);
        var outputString = codeableConceptCdMapper.getCdForTopic();

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForCategory_With_Title_Expect_OtherCategoryAndOriginalText() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_CATEGORY_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForCategory(TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_GetCdForCategory_Expect_OtherCategory() {
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_CATEGORY_NO_TITLE);
        var outputString = codeableConceptCdMapper.getCdForCategory();

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    @SneakyThrows
    public void When_MapToCdForMedication_With_RelatedProblemAndTitle_Expect_ConceptIdAndTitle() {
        var relatedProblem = ResourceTestFileUtils.getFileContent(TEST_FILE_TOPIC_RELATED_CONDITION);
        var codeableConcept = fhirParseService.parseResource(relatedProblem, Condition.class).getCode();
        var expectedOutput = ResourceTestFileUtils.getFileContent(CD_FOR_TOPIC_RELATED_PROBLEM_AND_TITLE);
        var outputString = codeableConceptCdMapper.mapToCdForTopic(codeableConcept, TEST_TITLE);

        assertThat(outputString).isEqualToIgnoringWhitespace(expectedOutput);
    }
}