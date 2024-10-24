package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.IdUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

@MockitoSettings(strictness = Strictness.LENIENT)
public class EncounterComponentsMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String PRACTITIONER_ID = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";

    private static final String TEST_DIRECTORY = "/ehr/mapper/encountercomponents/";
    private static final String INPUT_BUNDLE_WITH_ALL_MAPPERS_USED = TEST_DIRECTORY + "input-bundle-1.json";
    private static final String EXPECTED_COMPONENTS_MAPPED_WITH_ALL_MAPPERS_USED = TEST_DIRECTORY + "expected-components-1.xml";
    private static final String INPUT_BUNDLE_WITH_EMPTY_CONSULTATION_LIST = TEST_DIRECTORY + "input-bundle-2.json";
    private static final String INPUT_BUNDLE_WITH_EMPTY_TOPIC_LIST = TEST_DIRECTORY + "input-bundle-3.json";
    private static final String INPUT_BUNDLE_WITH_EMPTY_CATEGORY_LIST = TEST_DIRECTORY + "input-bundle-4.json";
    private static final String INPUT_BUNDLE_WITHOUT_CONSULTATION_LIST = TEST_DIRECTORY + "input-bundle-5.json";
    private static final String INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_MAPPERS_CRITERIA = TEST_DIRECTORY + "input-bundle-6.json";
    private static final String INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_BUNDLE = TEST_DIRECTORY + "input-bundle-7.json";
    private static final String INPUT_BUNDLE_WITH_NON_TOPIC_CONSULTATION_LIST_ENTRY = TEST_DIRECTORY + "input-bundle-8.json";
    private static final String INPUT_BUNDLE_WITH_UNSUPPORTED_RESOURCES = TEST_DIRECTORY + "input-bundle-9-unsupported-resource.json";
    private static final String INPUT_BUNDLE_WITH_IGNORED_RESOURCE = TEST_DIRECTORY + "input-bundle-10-ignored-resource.json";
    private static final String INPUT_BUNDLE_WITH_NON_CATEGORY_TOPIC_LIST_ENTRY = TEST_DIRECTORY
        + "input-bundle-11-invalid-category-list.json";
    private static final String INPUT_BUNDLE_WITH_RELATED_PROBLEM_IN_TOPIC = TEST_DIRECTORY + "input-bundle-12-related-problem.json";
    private static final String EXPECTED_COMPONENTS_RELATED_PROBLEM = TEST_DIRECTORY + "expected-components-12-related-problem.xml";
    private static final String INPUT_BUNDLE_WITH_NO_DATE_PROVIDED_IN_TOPIC = TEST_DIRECTORY
        + "input-bundle-13-topic-without-date.json";
    private static final String EXPECTED_COMPONENTS_TOPIC_AVAILABILITY_DATE_MAPPED_FROM_ENCOUNTER = TEST_DIRECTORY
        + "expected-components-13-topic-without-date.xml";
    private static final String INPUT_BUNDLE_WITH_MISSING_TITLE_IN_CATEGORY = TEST_DIRECTORY + "input-bundle-14-category-codes.json";
    private static final String EXPECTED_COMPONENTS_WITH_CD_IN_CATEGORIES = TEST_DIRECTORY + "expected-components-14-category-codes.xml";
    private static final String INPUT_BUNDLE_WITH_RELATED_PROBLEM_IN_TOPIC_NO_TITLE = TEST_DIRECTORY
        + "input-bundle-15-related-problem-no-title.json";
    private static final String EXPECTED_COMPONENTS_RELATED_PROBLEM_NO_TITLE = TEST_DIRECTORY
        + "expected-components-15-related-problem-no-title.xml";
    private static final String INPUT_BUNDLE_WITH_MISSING_TITLE_IN_TOPIC = TEST_DIRECTORY + "input-bundle-16-topic-codes.json";
    private static final String EXPECTED_COMPONENTS_WITH_CD_IN_TOPICS = TEST_DIRECTORY + "expected-components-16-topic-codes.xml";
    private static final String INPUT_BUNDLE_TOPIC_NO_CATEGORIES = TEST_DIRECTORY + "input-bundle-17-topic-no-categories.json";
    private static final String EXPECTED_COMPONENTS_TOPIC_NO_CATEGORIES = TEST_DIRECTORY
        + "expected-components-17-topic-no-categories.xml";
    private static final String CONTAINED_TEST_DIRECTORY = TEST_DIRECTORY + "contained-resources/";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private TimestampService timestampService;
    @Mock
    private BloodPressureValidator bloodPressureValidator;
    @Mock
    private ConfidentialityService confidentialityService;

    private EncounterComponentsMapper encounterComponentsMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptForMedication(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToNullFlavorCodeableConcept(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(any(CodeableConcept.class),
            any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(any(CodeableConcept.class),
            any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForTopic(any(String.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForCategory(any()))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(bloodPressureValidator.isValidBloodPressure(argThat(observation ->
            CodeableConceptMappingUtils.hasCode(observation.getCode(), List.of(
                "bloodPressureCode1", "bloodPressureCode2", "bloodPressureCode3"
            ))
        ))).thenReturn(true);
        lenient()
            .when(codeableConceptCdMapper.getCdForCategory())
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);

        ParticipantMapper participantMapper = new ParticipantMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();

        AllergyStructureMapper allergyStructureMapper = new AllergyStructureMapper(
            messageContext,
            codeableConceptCdMapper,
            participantMapper,
            confidentialityService
        );
        BloodPressureMapper bloodPressureMapper = new BloodPressureMapper(
            messageContext,
            randomIdGeneratorService,
            structuredObservationValueMapper,
            codeableConceptCdMapper,
            participantMapper
        );
        ConditionLinkSetMapper conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext,
            randomIdGeneratorService,
            codeableConceptCdMapper,
            participantMapper,
            confidentialityService
        );
        DiaryPlanStatementMapper diaryPlanStatementMapper
            = new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, participantMapper);
        DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper
            = new DocumentReferenceToNarrativeStatementMapper(
                messageContext, new SupportedContentTypes(), participantMapper, confidentialityService);
        MedicationStatementMapper medicationStatementMapper = new MedicationStatementMapper(
            messageContext,
            codeableConceptCdMapper,
            participantMapper,
            randomIdGeneratorService,
            confidentialityService
        );
        ObservationToNarrativeStatementMapper observationToNarrativeStatementMapper =
            new ObservationToNarrativeStatementMapper(messageContext, participantMapper);
        SpecimenMapper specimenMapper = getSpecimenMapper(structuredObservationValueMapper, participantMapper);

        ObservationStatementMapper observationStatementMapper = new ObservationStatementMapper(
            messageContext,
            structuredObservationValueMapper,
            new PertinentInformationObservationValueMapper(), codeableConceptCdMapper,
            participantMapper
        );
        ImmunizationObservationStatementMapper immunizationObservationStatementMapper =
            new ImmunizationObservationStatementMapper(
                messageContext,
                codeableConceptCdMapper,
                participantMapper,
                confidentialityService
            );
        RequestStatementMapper requestStatementMapper
            = new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper);
        DiagnosticReportMapper diagnosticReportMapper = new DiagnosticReportMapper(
            messageContext, specimenMapper, participantMapper, randomIdGeneratorService, confidentialityService
        );

        encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            allergyStructureMapper,
            bloodPressureMapper,
            conditionLinkSetMapper,
            diaryPlanStatementMapper,
            documentReferenceToNarrativeStatementMapper,
            immunizationObservationStatementMapper,
            medicationStatementMapper,
            observationToNarrativeStatementMapper,
            observationStatementMapper,
            requestStatementMapper,
            diagnosticReportMapper,
            bloodPressureValidator,
            codeableConceptCdMapper
        );
    }

    private @NotNull SpecimenMapper getSpecimenMapper(StructuredObservationValueMapper structuredObservationValueMapper,
                                                      ParticipantMapper participantMapper) {
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper,
            randomIdGeneratorService, confidentialityService);
        return new SpecimenMapper(messageContext, specimenObservationMapper, randomIdGeneratorService, confidentialityService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingEncounterComponents_Expect_ResourceMapped() {
        String expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_MAPPED_WITH_ALL_MAPPERS_USED);

        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_ALL_MAPPERS_USED);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        System.out.println(mappedXml);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingEncounterComponents_Expect_IgnoredResourceNotMapped() {
        String expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_MAPPED_WITH_ALL_MAPPERS_USED);

        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_IGNORED_RESOURCE);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);
        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("emptyResult")
    public void When_MappingEncounterComponents_Expect_NoResourceMapped(String inputJsonPath) {
        var bundle = initializeMessageContext(inputJsonPath);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);
        assertThat(mappedXml).isEmpty();
    }

    @Test
    public void When_MappingEncounterMissingComponents_Expect_ExceptionThrown() {
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_BUNDLE);
        var encounter = extractEncounter(bundle);

        assertThatThrownBy(() -> encounterComponentsMapper.mapComponents(encounter))
            .hasMessageContaining("Resource not found")
            .isInstanceOf(EhrMapperException.class);
    }

    @Test
    public void When_MappingEncounterUnsupportedResource_Expect_ExceptionThrown() {
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_UNSUPPORTED_RESOURCES);
        var encounter = extractEncounter(bundle);

        assertThatThrownBy(() -> encounterComponentsMapper.mapComponents(encounter))
            .hasMessageContaining("Unsupported resource in consultation list: Flag/flagid1")
            .isInstanceOf(EhrMapperException.class);
    }

    @Test
    public void When_MappingConsultation_WithNonTopicList_Expect_ExceptionThrown() {
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_NON_TOPIC_CONSULTATION_LIST_ENTRY);
        var encounter = extractEncounter(bundle);

        assertThatThrownBy(() ->
            encounterComponentsMapper.mapComponents(encounter))
            .hasMessageContaining("Unexpected list List/topicid1 referenced in Consultation, "
                + "expected list to be coded as Topic (EHR)")
            .isInstanceOf(EhrMapperException.class);
    }

    @Test
    public void When_MappingTopic_WithNonCategoryList_Expect_ExceptionThrown() {
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_NON_CATEGORY_TOPIC_LIST_ENTRY);
        var encounter = extractEncounter(bundle);

        assertThatThrownBy(() ->
            encounterComponentsMapper.mapComponents(encounter))
            .hasMessageContaining("Unexpected list List/category1 referenced in Consultation, "
                + "expected list to be coded as Category (EHR) or be a container")
            .isInstanceOf(EhrMapperException.class);
    }

    @Test
    public void When_MappingTopic_With_RelatedProblem_Expect_MappedToCode() {

        when(codeableConceptCdMapper.mapToCdForTopic(any(CodeableConcept.class), any(String.class)))
            .thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_RELATED_PROBLEM);
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_RELATED_PROBLEM_IN_TOPIC);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingTopic_With_RelatedProblemAndNoTitle_Expect_MappedToCode() {
        when(codeableConceptCdMapper.mapToCdForTopic(any(CodeableConcept.class)))
            .thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_RELATED_PROBLEM_NO_TITLE);
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_RELATED_PROBLEM_IN_TOPIC_NO_TITLE);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingTopic_With_MissingDate_Expect_DateMappedFromEncounter() {
        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_TOPIC_AVAILABILITY_DATE_MAPPED_FROM_ENCOUNTER);
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_NO_DATE_PROVIDED_IN_TOPIC);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingCategory_WithAndWithout_Title_Expect_BothCdsPresent() {
        when(codeableConceptCdMapper.getCdForCategory()).thenCallRealMethod();
        when(codeableConceptCdMapper.mapToCdForCategory(any())).thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_WITH_CD_IN_CATEGORIES);
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_MISSING_TITLE_IN_CATEGORY);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingTopic_WithAndWithout_Title_Expect_BothCdsPresent() {

        when(codeableConceptCdMapper.mapToCdForTopic(any(String.class))).thenCallRealMethod();
        when(codeableConceptCdMapper.getCdForTopic()).thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_WITH_CD_IN_TOPICS);
        var bundle = initializeMessageContext(INPUT_BUNDLE_WITH_MISSING_TITLE_IN_TOPIC);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void When_MappingTopic_WithoutCategory_Expect_ComponentsMapped() {
        var expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_TOPIC_NO_CATEGORIES);
        var bundle = initializeMessageContext(INPUT_BUNDLE_TOPIC_NO_CATEGORIES);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("containedResourceMappingArguments")
    public void When_MappingContainedResource_Expect_ResourcesMapped(String inputBundle, String expectedComponents) {
        var expectedXml = ResourceTestFileUtils.getFileContent(CONTAINED_TEST_DIRECTORY + expectedComponents);
        var bundle = initializeMessageContext(CONTAINED_TEST_DIRECTORY + inputBundle);
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        System.out.println(mappedXml);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void When_MappingWithRelatedProblemWithIncorrectProblemExtensionUrl_Expect_UnspecifiedProblemWithOriginalText() {
        when(codeableConceptCdMapper.mapToCdForTopic(anyString()))
            .thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(
            TEST_DIRECTORY + "expected-components-18-related-problem-invalid-extension.xml"
        );
        var bundle = initializeMessageContext(
            TEST_DIRECTORY + "input-bundle-18-related-problem-invalid-problem-extension.json"
        );
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void When_MappingWithRelatedProblemWithIncorrectProblemExtensionExtensionUrl_Expect_UnspecifiedProblemWithOriginalText() {
        when(codeableConceptCdMapper.mapToCdForTopic(anyString()))
            .thenCallRealMethod();

        var expectedXml = ResourceTestFileUtils.getFileContent(
            TEST_DIRECTORY + "expected-components-18-related-problem-invalid-extension.xml"
        );
        var bundle = initializeMessageContext(
            TEST_DIRECTORY + "input-bundle-19-related-problem-invalid-problem-extension-extension-url.json"
        );
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void When_MapResourceToComponent_With_UnsupportedResource_Expect_PlaceholderCommentProduced() {
        var resource = new QuestionnaireResponse()
            .setIdElement(
                IdUtil.buildIdType(ResourceType.QuestionnaireResponse, "questionnaire-response-id")
            );

        var expectedComponent = "<!-- QuestionnaireResponse/questionnaire-response-id -->";

        var actualComponent = encounterComponentsMapper.mapResourceToComponent(resource);

        assertThat(actualComponent)
            .isEqualTo(Optional.of(expectedComponent));
    }

    @Test
    void When_MappingTopic_With_StoppedMedicationRequest_Expect_MedicationStatementNotIncludedInOutput() {
        // GP2GP Currently has no mechanism to transfer the concept that a Medication has been "stopped"
        // Until there is some way to convey this, it makes more sense to not send the misleading medication.
        var expectedXml = ResourceTestFileUtils.getFileContent(
            TEST_DIRECTORY + "expected-components-20-medication-statement-not-included.xml"
        );
        var bundle = initializeMessageContext(
            TEST_DIRECTORY + "input-bundle-20-medication-request-stopped-order.json"
        );
        var encounter = extractEncounter(bundle);

        String mappedXml = encounterComponentsMapper.mapComponents(encounter);

        assertThat(mappedXml)
            .isEqualToIgnoringWhitespace(expectedXml);
    }

    private static Stream<Arguments> containedResourceMappingArguments() {
        return Stream.of(
            Arguments.of("input-referenced-in-category.json", "output-referenced-in-category.xml"),
            Arguments.of("input-referenced-in-topic.json", "output-referenced-in-topic.xml"),
            Arguments.of("input-not-referenced.json", "output-empty.xml"),
            Arguments.of("input-topic-and-category.json", "output-topic-and-category.xml"),
            Arguments.of("input-two-resources-category.json", "output-two-resources-category.xml"),
            Arguments.of("input-referenced-observation.json", "output-referenced-observation.xml")
        );
    }

    private Encounter extractEncounter(Bundle bundle) {
        return (Encounter) bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.Encounter.equals(resource.getResourceType()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Encounter not found in test fixture"));
    }

    private static Stream<Arguments> emptyResult() {
        return Stream.of(
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_CONSULTATION_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_TOPIC_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_CATEGORY_LIST),
            Arguments.of(INPUT_BUNDLE_WITHOUT_CONSULTATION_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_MAPPERS_CRITERIA)
        );
    }

    private Bundle initializeMessageContext(String inputJsonPath) {
        String inputJson = ResourceTestFileUtils.getFileContent(inputJsonPath);
        Bundle bundle = new FhirParseService().parseResource(inputJson, Bundle.class);
        messageContext.initialize(bundle);

        IdType conditionId = buildIdType(ResourceType.Practitioner, PRACTITIONER_ID);
        messageContext.getAgentDirectory().getAgentId(new Reference(conditionId));

        return bundle;
    }
}
