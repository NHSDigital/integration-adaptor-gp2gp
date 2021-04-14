package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class ConditionLinkSetMapperTest {

    private static final String CONDITION_ID = "7E277DF1-6F1C-47CD-84F7-E9B7BF4105DB-PROB";
    private static final String GENERATED_ID = "50233a2f-128f-4b96-bdae-6207ed11a8ea";
    private static final String CONDITION_FILE_LOCATIONS = "/ehr/mapper/condition/";
    private static final String INPUT_JSON_BUNDLE =  CONDITION_FILE_LOCATIONS + "fhir-bundle.json";

    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION = CONDITION_FILE_LOCATIONS + "condition_all_included.json";
    private static final String INPUT_JSON_NO_ACTUAL_PROBLEM = CONDITION_FILE_LOCATIONS + "condition_no_problem.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION = CONDITION_FILE_LOCATIONS
        + "condition_actual_problem_condition.json";
    private static final String INPUT_JSON_WITH_MAJOR_SIGNIFICANCE = CONDITION_FILE_LOCATIONS
        + "condition_major_significance.json";
    private static final String INPUT_JSON_WITH_MINOR_SIGNIFICANCE = CONDITION_FILE_LOCATIONS + "condition_all_included.json";
    private static final String INPUT_JSON_NO_RELATED_CLINICAL_CONTENT = CONDITION_FILE_LOCATIONS
        + "condition_no_related_clinical_content.json";
    private static final String INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT_IGNORE_ONE = CONDITION_FILE_LOCATIONS
        + "condition_related_clinical_content_1_resource_1_list_reference.json";
    private static final String INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT = CONDITION_FILE_LOCATIONS
        + "condition_2_related_clinical_content.json";
    private static final String INPUT_JSON_STATUS_ACTIVE = CONDITION_FILE_LOCATIONS + "condition_status_active.json";
    private static final String INPUT_JSON_STATUS_INACTIVE = CONDITION_FILE_LOCATIONS + "condition_status_inactive.json";
    private static final String INPUT_JSON_DATES_PRESENT = CONDITION_FILE_LOCATIONS + "condition_dates_present.json";
    private static final String INPUT_JSON_DATES_NOT_PRESENT = CONDITION_FILE_LOCATIONS + "condition_dates_not_present.json";
    private static final String INPUT_JSON_RELATED_CLINICAL_CONTENT_LIST_REFERENCE = CONDITION_FILE_LOCATIONS
        + "condition_related_clinical_content_list_reference.json";
    private static final String INPUT_JSON_RELATED_CLINICAL_CONTENT_NON_EXISTENT_REFERENCE = CONDITION_FILE_LOCATIONS
        + "condition_related_clinical_content_non_existent_reference.json";

    private static final String EXPECTED_OUTPUT_LINKSET = CONDITION_FILE_LOCATIONS + "expected_output_linkset_";
    private static final String OUTPUT_XML_WITH_IS_NESTED = EXPECTED_OUTPUT_LINKSET + "1.xml";
    private static final String OUTPUT_XML_WITHOUT_IS_NESTED = EXPECTED_OUTPUT_LINKSET + "2.xml";
    private static final String OUTPUT_XML_WITH_GENERATED_PROBLEM_IS_NESTED = EXPECTED_OUTPUT_LINKSET + "3.xml";
    private static final String OUTPUT_XML_WITH_CONDITION_NAMED = EXPECTED_OUTPUT_LINKSET + "4.xml";
    private static final String OUTPUT_XML_WITH_CONDITION_NAMED_OBSERVATION_STATEMENT_GENERATED = EXPECTED_OUTPUT_LINKSET + "5.xml";
    private static final String OUTPUT_XML_WITH_MAJOR_SIGNIFICANCE = EXPECTED_OUTPUT_LINKSET + "6.xml";
    private static final String OUTPUT_XML_WITH_MINOR_SIGNIFICANCE = EXPECTED_OUTPUT_LINKSET + "7.xml";
    private static final String OUTPUT_XML_WITH_NO_RELATED_CLINICAL_CONTENT = EXPECTED_OUTPUT_LINKSET + "8.xml";
    private static final String OUTPUT_XML_WITH_1_RELATED_CLINICAL_CONTENT = EXPECTED_OUTPUT_LINKSET + "9.xml";
    private static final String OUTPUT_XML_WITH_2_RELATED_CLINICAL_CONTENT = EXPECTED_OUTPUT_LINKSET + "10.xml";
    private static final String OUTPUT_XML_WITH_STATUS_ACTIVE = EXPECTED_OUTPUT_LINKSET + "11.xml";
    private static final String OUTPUT_XML_WITH_STATUS_INACTIVE = EXPECTED_OUTPUT_LINKSET + "12.xml";
    private static final String OUTPUT_XML_WITH_DATES_PRESENT = EXPECTED_OUTPUT_LINKSET + "13.xml";
    private static final String OUTPUT_XML_WITH_DATES_NOT_PRESENT = EXPECTED_OUTPUT_LINKSET + "14.xml";

    @Mock
    private IdMapper idMapper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private InputBundle inputBundle;
    private ConditionLinkSetMapper conditionLinkSetMapper;
    private FhirParseService fhirParseService;
    private Bundle bundle;

    @BeforeEach
    public void setUp() throws IOException {
        fhirParseService = new FhirParseService();

        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        inputBundle = new InputBundle(bundle);

        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(idMapper.getOrNew(ResourceType.Condition, CONDITION_ID)).thenReturn(CONDITION_ID);

        conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext, randomIdGeneratorService, codeableConceptCdMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void When_MappingParsedConditionWithRealProblem_Expect_LinkSetXml(String conditionJson, String outputXml, boolean isNested)
        throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(conditionJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        Condition condition = fhirParseService.parseResource(jsonInput, Condition.class);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
        when(idMapper.getOrNew(any(Reference.class))).thenAnswer(answerWithObjectId());
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        String outputMessage = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);
        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITH_IS_NESTED, true),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITHOUT_IS_NESTED, false),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION, OUTPUT_XML_WITH_CONDITION_NAMED, false),
            Arguments.of(INPUT_JSON_WITH_MAJOR_SIGNIFICANCE, OUTPUT_XML_WITH_MAJOR_SIGNIFICANCE, false),
            Arguments.of(INPUT_JSON_WITH_MINOR_SIGNIFICANCE, OUTPUT_XML_WITH_MINOR_SIGNIFICANCE, false),
            Arguments.of(INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT_IGNORE_ONE, OUTPUT_XML_WITH_1_RELATED_CLINICAL_CONTENT, false),
            Arguments.of(INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT, OUTPUT_XML_WITH_2_RELATED_CLINICAL_CONTENT, false),
            Arguments.of(INPUT_JSON_STATUS_ACTIVE, OUTPUT_XML_WITH_STATUS_ACTIVE, false),
            Arguments.of(INPUT_JSON_STATUS_INACTIVE, OUTPUT_XML_WITH_STATUS_INACTIVE, false),
            Arguments.of(INPUT_JSON_DATES_PRESENT, OUTPUT_XML_WITH_DATES_PRESENT, false),
            Arguments.of(INPUT_JSON_DATES_NOT_PRESENT, OUTPUT_XML_WITH_DATES_NOT_PRESENT, false),
            Arguments.of(INPUT_JSON_RELATED_CLINICAL_CONTENT_LIST_REFERENCE, OUTPUT_XML_WITH_NO_RELATED_CLINICAL_CONTENT, false)
        );
    }

    @ParameterizedTest
    @MethodSource("testObservationArguments")
    public void When_MappingParsedConditionWithObservation_Expect_LinkSetXml(String conditionJson, String outputXml, boolean isNested)
        throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(conditionJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        Condition condition = fhirParseService.parseResource(jsonInput, Condition.class);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
        when(randomIdGeneratorService.createNewId()).thenReturn(GENERATED_ID);
        when(idMapper.getOrNew(any(Reference.class))).thenAnswer(answerWithObjectId());
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        String outputMessage = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);
        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> testObservationArguments() {
        return Stream.of(
            Arguments.of(INPUT_JSON_NO_ACTUAL_PROBLEM, OUTPUT_XML_WITH_GENERATED_PROBLEM_IS_NESTED, true),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION, OUTPUT_XML_WITH_CONDITION_NAMED_OBSERVATION_STATEMENT_GENERATED, false)
        );
    }

    private Answer<String> answerWithObjectId() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return reference.getReferenceElement().getIdPart();
        };
    }

    @Test
    public void When_MappingParsedConditionWithNoRelatedClinicalContent_Expect_LinkSetXml()
        throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_NO_RELATED_CLINICAL_CONTENT);
        var expectedOutput = ResourceTestFileUtils.getFileContent(OUTPUT_XML_WITH_NO_RELATED_CLINICAL_CONTENT);
        Condition condition = fhirParseService.parseResource(jsonInput, Condition.class);

        when(idMapper.getOrNew(any(Reference.class))).thenAnswer(answerWithObjectId());
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        String outputMessage = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);
        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingParsedConditionWithNonExistentReferenceInRelatedClinicalContent_Expect_MapperException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_RELATED_CLINICAL_CONTENT_NON_EXISTENT_REFERENCE);
        Condition parsedObservation = fhirParseService.parseResource(jsonInput, Condition.class);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(parsedObservation, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Could not resolve Condition Related Medical Content reference");
    }
}
