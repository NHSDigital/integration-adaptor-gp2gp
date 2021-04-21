package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
public class EncounterComponentsMapperTest {
    private static final String TEST_ID = "394559384658936";

    private static final String TEST_DIRECTORY = "/ehr/mapper/encountercomponents/";
    private static final String INPUT_BUNDLE_WITH_ALL_MAPPERS_USED = TEST_DIRECTORY + "input-bundle-1.json";
    private static final String EXPECTED_COMPONENTS_MAPPED_WITH_ALL_MAPPERS_USED = TEST_DIRECTORY + "expected-components-1.xml";
    private static final String INPUT_BUNDLE_WITH_EMPTY_CONSULTATION_LIST = TEST_DIRECTORY + "input-bundle-2.json";
    private static final String INPUT_BUNDLE_WITH_EMPTY_TOPIC_LIST = TEST_DIRECTORY + "input-bundle-3.json";
    private static final String INPUT_BUNDLE_WITH_EMPTY_CATEGORY_LIST = TEST_DIRECTORY + "input-bundle-4.json";
    private static final String INPUT_BUNDLE_WITHOUT_CONSULTATION_LIST = TEST_DIRECTORY + "input-bundle-5.json";
    private static final String INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_MAPPERS_CRITERIA = TEST_DIRECTORY + "input-bundle-6.json";
    private static final String INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_BUNDLE = TEST_DIRECTORY + "input-bundle-7.json";
    private static final String INPUT_BUNDLE_WITH_LIST_NOT_IN_TOPIC_OR_CATEGORY = TEST_DIRECTORY + "input-bundle-8.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private EncounterComponentsMapper encounterComponentsMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.getIdMapper().getOrNew(ResourceType.Practitioner, "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73");

        ParticipantMapper participantMapper = new ParticipantMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();

        AllergyStructureMapper allergyStructureMapper
            = new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper);
        BloodPressureMapper bloodPressureMapper = new BloodPressureMapper(
            messageContext,
            randomIdGeneratorService,
            structuredObservationValueMapper,
            codeableConceptCdMapper
        );
        ConditionLinkSetMapper conditionLinkSetMapper = new ConditionLinkSetMapper(messageContext,
            randomIdGeneratorService,
            codeableConceptCdMapper,
            participantMapper
        );
        DiaryPlanStatementMapper diaryPlanStatementMapper = new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper);
        DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper
            = new DocumentReferenceToNarrativeStatementMapper(messageContext);
        MedicationStatementMapper medicationStatementMapper
            = new MedicationStatementMapper(messageContext, codeableConceptCdMapper, randomIdGeneratorService);
        ObservationToNarrativeStatementMapper observationToNarrativeStatementMapper =
            new ObservationToNarrativeStatementMapper(messageContext, participantMapper);
        ObservationStatementMapper observationStatementMapper = new ObservationStatementMapper(
            messageContext,
            structuredObservationValueMapper,
            new PertinentInformationObservationValueMapper(), codeableConceptCdMapper,
            participantMapper);
        ImmunizationObservationStatementMapper immunizationObservationStatementMapper =
            new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper);
        RequestStatementMapper requestStatementMapper
            = new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper);

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
            requestStatementMapper
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingEncounterComponents_Expect_ResourceMapped() throws IOException {
        String expectedXml = ResourceTestFileUtils.getFileContent(EXPECTED_COMPONENTS_MAPPED_WITH_ALL_MAPPERS_USED);

        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_BUNDLE_WITH_ALL_MAPPERS_USED);
        Bundle bundle = new FhirParseService().parseResource(inputJson, Bundle.class);
        messageContext.initialize(bundle);

        Optional<Encounter> encounter = extractEncounter(bundle);
        assertThat(encounter.isPresent()).isTrue();

        String mappedXml = encounterComponentsMapper.mapComponents(encounter.get());
        assertThat(mappedXml).isEqualTo(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("emptyResult")
    public void When_MappingEncounterComponents_Expect_NoResourceMapped(String inputJsonPath) throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(inputJsonPath);
        Bundle bundle = new FhirParseService().parseResource(inputJson, Bundle.class);
        messageContext.initialize(bundle);

        Optional<Encounter> encounter = extractEncounter(bundle);
        assertThat(encounter.isPresent()).isTrue();

        String mappedXml = encounterComponentsMapper.mapComponents(encounter.get());
        assertThat(mappedXml).isEmpty();
    }

    private Optional<Encounter> extractEncounter(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, Encounter.class)
            .findFirst();
    }

    private static Stream<Arguments> emptyResult() {
        return Stream.of(
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_CONSULTATION_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_TOPIC_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_EMPTY_CATEGORY_LIST),
            Arguments.of(INPUT_BUNDLE_WITHOUT_CONSULTATION_LIST),
            Arguments.of(INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_MAPPERS_CRITERIA),
            Arguments.of(INPUT_BUNDLE_WITH_RESOURCES_NOT_IN_BUNDLE),
            Arguments.of(INPUT_BUNDLE_WITH_LIST_NOT_IN_TOPIC_OR_CATEGORY)
        );
    }
}
