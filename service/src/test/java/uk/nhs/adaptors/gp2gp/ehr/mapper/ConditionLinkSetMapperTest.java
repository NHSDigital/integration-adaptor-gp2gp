package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import wiremock.org.custommonkey.xmlunit.XMLAssert;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOSCRUB;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;
import static uk.nhs.adaptors.gp2gp.utils.XmlAssertion.assertThatXml;
import static uk.nhs.adaptors.gp2gp.utils.XmlParsingUtility.wrapXmlInRootElement;

@ExtendWith(MockitoExtension.class)
class ConditionLinkSetMapperTest {

    private static final String CONDITION_ID = "7E277DF1-6F1C-47CD-84F7-E9B7BF4105DB-PROB";
    private static final String GENERATED_ID = "50233a2f-128f-4b96-bdae-6207ed11a8ea";
    private static final String ALLERGY_ID = "230D3D37-99E3-450A-AE88-B5AB802B7137";
    private static final String IMMUNIZATION_ID = "C93659E1-1107-441C-BE25-C5EF4B7831D1";
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/condition/";

    private static final String INPUT_JSON_BUNDLE = "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_OBSERVATION = "condition_all_included.json";
    private static final String INPUT_JSON_NO_ACTUAL_PROBLEM = "condition_no_problem.json";
    private static final String INPUT_JSON_NO_ACTUAL_PROBLEM_NO_DATE = "condition_no_problem_no_onsetdate.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION = "condition_actual_problem_condition.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_ALLERGY_INTOLERANCE =
        "condition_actual_problem_allergy_intolerance.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_IMMUNIZATION = "condition_actual_problem_immunization.json";
    private static final String INPUT_JSON_WITH_MAJOR_SIGNIFICANCE = "condition_major_significance.json";
    private static final String INPUT_JSON_WITH_MINOR_SIGNIFICANCE = "condition_all_included.json";
    private static final String INPUT_JSON_NO_RELATED_CLINICAL_CONTENT = "condition_no_related_clinical_content.json";
    private static final String INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT_IGNORE_ONE =
        "condition_related_clinical_content_suppressed_linkage_references.json";
    private static final String INPUT_JSON_MAP_TWO_RELATED_CLINICAL_CONTENT = "condition_2_related_clinical_content.json";
    private static final String INPUT_JSON_STATUS_ACTIVE = "condition_status_active.json";
    private static final String INPUT_JSON_STATUS_INACTIVE = "condition_status_inactive.json";
    private static final String INPUT_JSON_DATES_PRESENT = "condition_dates_present.json";
    private static final String INPUT_JSON_DATES_NOT_PRESENT = "condition_dates_not_present.json";
    private static final String INPUT_JSON_RELATED_CLINICAL_CONTENT_LIST_REFERENCE =
        "condition_related_clinical_content_list_reference.json";
    private static final String INPUT_JSON_RELATED_CLINICAL_CONTENT_NON_EXISTENT_REFERENCE =
        "condition_related_clinical_content_non_existent_reference.json";
    private static final String INPUT_JSON_ASSERTER_NOT_PRESENT = "condition_asserter_not_present.json";
    private static final String INPUT_JSON_ASSERTER_NOT_PRACTITIONER = "condition_asserter_not_practitioner.json";
    private static final String INPUT_JSON_MISSING_CONDITION_CODE = "condition_missing_code.json";
    private static final String INPUT_JSON_SUPPRESSED_RELATED_MEDICATION_REQUEST =
        "condition_suppressed_related_medication_request.json";
    private static final String INPUT_JSON_RELATED_CLINICAL_CONTENT_ALLERGY = "condition_related_clinical_content_allergy.json";
    private static final String INPUT_JSON_WITH_ACTUAL_PROBLEM_MEDICATION_REQUEST =
        "condition_actual_problem_medication_request.json";

    private static final String EXPECTED_OUTPUT_LINKSET = "expected_output_linkset_";
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
    private static final String OUTPUT_XML_SUPPRESSED_RELATED_MEDICATION_REQUEST = EXPECTED_OUTPUT_LINKSET + "15.xml";
    private static final String OUTPUT_XML_ALLERGY_INTOLERANCE_ACTUAL_PROBLEM = EXPECTED_OUTPUT_LINKSET + "16.xml";
    private static final String OUTPUT_XML_IMMUNIZATION_ACTUAL_PROBLEM = EXPECTED_OUTPUT_LINKSET + "17.xml";
    private static final String OUTPUT_XML_WITH_NULL_FLAVOR_OBSERVATION_STATEMENT_AVAILABILITY_TIME = EXPECTED_OUTPUT_LINKSET + "18.xml";
    private static final String OUTPUT_XML_WITH_STATEMENT_REF_LINK_ALLERGY_OBSERVATION = EXPECTED_OUTPUT_LINKSET + "19.xml";
    private static final String OUTPUT_XML_MEDICATION_REQUEST_ACTUAL_PROBLEM = EXPECTED_OUTPUT_LINKSET + "20.xml";
    private static final String OUTPUT_XML_NO_PARTICIPANT = EXPECTED_OUTPUT_LINKSET + "21.xml";

    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private MessageContext messageContext;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private ConfidentialityService confidentialityService;
    @Captor
    private ArgumentCaptor<Condition> conditionArgumentCaptor;

    private InputBundle inputBundle;
    private ConditionLinkSetMapper conditionLinkSetMapper;

    @BeforeEach
    void setUp() throws IOException {
        var bundleInput = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + INPUT_JSON_BUNDLE);
        final Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        inputBundle = new InputBundle(bundle);

        lenient().when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        lenient().when(codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(any(CodeableConcept.class)))
                .thenReturn(CodeableConceptMapperMockUtil.ACTUAL_PROBLEM_CODE);
        lenient().when(messageContext.getIdMapper()).thenReturn(idMapper);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        IdType conditionId = buildIdType(ResourceType.Condition, CONDITION_ID);
        IdType allergyId = buildIdType(ResourceType.AllergyIntolerance, ALLERGY_ID);
        IdType immunizationId = buildIdType(ResourceType.Immunization, IMMUNIZATION_ID);
        lenient().when(idMapper.getOrNew(ResourceType.Condition, conditionId)).thenReturn(CONDITION_ID);
        lenient().when(idMapper.getOrNew(ResourceType.Observation, allergyId)).thenReturn(ALLERGY_ID);
        lenient().when(idMapper.getOrNew(ResourceType.Observation, immunizationId)).thenReturn(IMMUNIZATION_ID);
        lenient().when(idMapper.getOrNew(any(Reference.class))).thenAnswer(answerWithObjectId(ResourceType.Condition));
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(answerWithObjectId());
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(GENERATED_ID);


        conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext, randomIdGeneratorService, codeableConceptCdMapper,
            new ParticipantMapper(), confidentialityService);
    }

    @AfterEach
    void afterEach() {
        messageContext.resetMessageContext();
    }

    @Test
    void When_MappingParsedConditionWithoutMappedAgent_Expect_EhrMapperException() {
        final EhrMapperException propagatedException = new EhrMapperException("expected exception");
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_STATUS_ACTIVE);

        when(agentDirectory.getAgentId(any(Reference.class)))
            .thenThrow(propagatedException);

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isSameAs(propagatedException);
    }

    @Test
    void When_MappingParsedConditionWithAsserterNotPractitioner_Expect_EhrMapperException() {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_ASSERTER_NOT_PRACTITIONER);

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Condition.asserter must be a Practitioner");
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    void When_MappingParsedCondition_With_RealProblem_Expect_LinkSetXml(String conditionJson, String outputXml, boolean isNested) {
        final Condition condition = getConditionResourceFromJson(conditionJson);
        final String expectedXml = getXmlStringFromFile(outputXml);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("testObservationArguments")
    void When_MappingParsedCondition_With_ActualProblemContentAndIsObservation_Expect_LinkSetXml(String conditionJson,
                                                                                                 String outputXml, boolean isNested) {
        final Condition condition = getConditionResourceFromJson(conditionJson);
        final String expectedXml = getXmlStringFromFile(outputXml);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, isNested);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedCondition_With_NoRelatedClinicalContent_Expect_LinkSetXml() {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_NO_RELATED_CLINICAL_CONTENT);
        final String expectedXml = getXmlStringFromFile(OUTPUT_XML_WITH_NO_RELATED_CLINICAL_CONTENT);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingParsedCondition_With_NonExistentReferenceInRelatedClinicalContent_Expect_MapperException() {
        final Condition parsedObservation = getConditionResourceFromJson(INPUT_JSON_RELATED_CLINICAL_CONTENT_NON_EXISTENT_REFERENCE);

        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);

        // TODO: workaround for NIAD-1409 should throw an exception but demonstrator include invalid references
        assumeThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(parsedObservation, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Could not resolve Condition Related Medical Content reference");
    }

    @Test
    void When_MappingParsedConditionCodeIsMissing_Expect_MapperException() {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_MISSING_CONDITION_CODE);

        assertThatThrownBy(() -> conditionLinkSetMapper.mapConditionToLinkSet(condition, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Condition code not present");
    }

    @Test
    void When_MappingCondition_With_SuppressedMedReqAsRelatedClinicalContent_Expect_NoEntry() throws IOException, SAXException {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_SUPPRESSED_RELATED_MEDICATION_REQUEST);
        final String expectedXml = getXmlStringFromFile(OUTPUT_XML_SUPPRESSED_RELATED_MEDICATION_REQUEST);

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);

        XMLAssert.assertXMLEqual(expectedXml, actualXml);
    }

    @Test
    void When_MappingCondition_With_NopatMetaSecurity_Expect_ConfidentialityCodeInBothLinksetAndObservationStatement() {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION);
        final String linkSetXpath = "/Root/component[1]/LinkSet/" + ConfidentialityCodeUtility.getNopatConfidentialityCodeXpathSegment();
        final String observationStatementXpath = "/Root/component[2]/ObservationStatement/" + ConfidentialityCodeUtility
            .getNopatConfidentialityCodeXpathSegment();

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(condition);
        when(confidentialityService.generateConfidentialityCode(conditionArgumentCaptor.capture()))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));

        final String actualXml = wrapXmlInRootElement(conditionLinkSetMapper
            .mapConditionToLinkSet(condition, false));
        final String conditionSecurityCode = ConfidentialityCodeUtility
            .getSecurityCodeFromResource(conditionArgumentCaptor.getValue());

        assertAll(
            () -> assertThatXml(actualXml).containsXPath(observationStatementXpath),
            () -> assertThatXml(actualXml).containsXPath(linkSetXpath),
            () -> assertThat(conditionSecurityCode).isEqualTo(NOPAT)
        );
    }

    @Test
    void When_MappingCondition_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeNotPresent() {
        final Condition condition = getConditionResourceFromJson(INPUT_JSON_SUPPRESSED_RELATED_MEDICATION_REQUEST);

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(condition);
        when(confidentialityService.generateConfidentialityCode(conditionArgumentCaptor.capture()))
            .thenReturn(Optional.empty());

        final String actualXml = conditionLinkSetMapper.mapConditionToLinkSet(condition, false);
        final String conditionSecurityCode = ConfidentialityCodeUtility
            .getSecurityCodeFromResource(conditionArgumentCaptor.getValue());

        assertThat(actualXml).doesNotContainIgnoringCase(NOPAT_HL7_CONFIDENTIALITY_CODE);
        assertThat(conditionSecurityCode).isEqualTo(NOSCRUB);
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
            Arguments.of(INPUT_JSON_ASSERTER_NOT_PRESENT, OUTPUT_XML_NO_PARTICIPANT, false),
            Arguments.of(INPUT_JSON_RELATED_CLINICAL_CONTENT_LIST_REFERENCE, OUTPUT_XML_WITH_NO_RELATED_CLINICAL_CONTENT, false),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_ALLERGY_INTOLERANCE, OUTPUT_XML_ALLERGY_INTOLERANCE_ACTUAL_PROBLEM, false),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_IMMUNIZATION, OUTPUT_XML_IMMUNIZATION_ACTUAL_PROBLEM, false),
            Arguments.of(INPUT_JSON_RELATED_CLINICAL_CONTENT_ALLERGY, OUTPUT_XML_WITH_STATEMENT_REF_LINK_ALLERGY_OBSERVATION, false),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_MEDICATION_REQUEST, OUTPUT_XML_MEDICATION_REQUEST_ACTUAL_PROBLEM, false)
        );
    }

    private static Stream<Arguments> testObservationArguments() {
        return Stream.of(
            Arguments.of(INPUT_JSON_NO_ACTUAL_PROBLEM, OUTPUT_XML_WITH_GENERATED_PROBLEM_IS_NESTED, true),
            Arguments.of(INPUT_JSON_WITH_ACTUAL_PROBLEM_CONDITION, OUTPUT_XML_WITH_CONDITION_NAMED_OBSERVATION_STATEMENT_GENERATED, false),
            Arguments.of(INPUT_JSON_NO_ACTUAL_PROBLEM_NO_DATE, OUTPUT_XML_WITH_NULL_FLAVOR_OBSERVATION_STATEMENT_AVAILABILITY_TIME,
                true)
        );
    }

    private Answer<String> answerWithObjectId(ResourceType type) {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("%s-%s-new-ID", reference.getReferenceElement().getIdPart(), type.name());
        };
    }

    private Answer<String> answerWithObjectId() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("%s-%s-new-ID",
                reference.getReferenceElement().getIdPart(),
                reference.getReferenceElement().getResourceType());
        };
    }

    private Condition getConditionResourceFromJson(String filename) {
        final String filePath = TEST_FILES_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Condition.class);
    }

    private String getXmlStringFromFile(String filename) {
        return ResourceTestFileUtils.getFileContent(
            TEST_FILES_DIRECTORY + filename
        );
    }
}