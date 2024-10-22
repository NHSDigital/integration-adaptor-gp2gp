package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
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
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EhrExtractMapperComponentTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;

    private static final String JSON_INPUT_FILE = "gpc-access-structured.json";
    private static final String DUPLICATE_RESOURCE_BUNDLE = INPUT_PATH + "duplicated-resource-bundle.json";
    private static final String ONE_CONSULTATION_RESOURCE_BUNDLE = INPUT_PATH + "1-consultation-resource.json";
    private static final String FHIR_BUNDLE_WITHOUT_EFFECTIVE_TIME = "fhir-bundle-without-effective-time.json";
    private static final String FHIR_BUNDLE_WITHOUT_HIGH_EFFECTIVE_TIME = "fhir-bundle-without-high-effective-time.json";
    private static final String FHIR_BUNDLE_WITH_EFFECTIVE_TIME = "fhir-bundle-with-effective-time.json";

    private static final String FHIR_BUNDLE_WITH_WITH_OBSERVATIONS_BEFORE_DIAGNOSTIC_REPORT =
        "fhir-bundle-observations-before-diagnostic-report.json";
    private static final String FHIR_BUNDLE_WITH_WITH_OBSERVATIONS_AFTER_DIAGNOSTIC_REPORT =
        "fhir-bundle-observations-after-diagnostic-report.json";
    private static final String FHIR_BUNDLE_WITH_OBSERVATIONS_UNRELATED_TO_DIAGNOSTIC_REPORT =
        "fhir-bundle-observations-unrelated-to-diagnostic-report.json";
    private static final String FHIR_BUNDLE_WITH_OBSERVATIONS_WITH_RELATED_OBSERVATIONS =
            "fhir-bundle-observations-with-related-observations.json";

    private static final String EXPECTED_XML_FOR_ONE_CONSULTATION_RESOURCE = "ExpectedResponseFrom1ConsultationResponse.xml";

    private static final String EXPECTED_XML_TO_JSON_FILE = "expected-ehr-extract-response-from-json.xml";
    private static final String EXPECTED_XML_WITHOUT_EFFECTIVE_TIME = "expected-xml-without-effective-time.xml";
    private static final String EXPECTED_XML_WITHOUT_HIGH_EFFECTIVE_TIME = "expected-xml-without-high-effective-time.xml";
    private static final String EXPECTED_XML_WITH_EFFECTIVE_TIME = "expected-xml-with-effective-time.xml";
    private static final String EXPECTED_XML_WITH_OBSERVATIONS_INSIDE_REPORT = "expected-xml-with-observations-inside-report.xml";
    private static final String EXPECTED_XML_WITH_STANDALONE_OBSERVATIONS = "expected-xml-with-standalone-observations.xml";
    private static final String EXPECTED_XML_WITH_RELATED_OBSERVATIONS = "expected-xml-with-related-observations.xml";

    private static final String TEST_ID_1 = "test-id-1";
    private static final String TEST_ID_2 = "test-id-2";
    private static final String TEST_ID_3 = "test-id-3";
    private static final String TEST_CONVERSATION_ID = "test-conversation-id";
    private static final String TEST_REQUEST_ID = "test-request-id";
    private static final String TEST_NHS_NUMBER = "1234567890";
    private static final String TEST_FROM_ODS_CODE = "test-from-ods-code";
    private static final String TEST_TO_ODS_CODE = "test-to-ods-code";
    private static final String TEST_DATE_TIME = "2020-01-01T01:01:01.01Z";

    private static GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private ConfidentialityService confidentialityService;

    private NonConsultationResourceMapper nonConsultationResourceMapper;
    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(TEST_NHS_NUMBER)
            .conversationId(TEST_CONVERSATION_ID)
            .requestId(TEST_REQUEST_ID)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .build();

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID_1, TEST_ID_2, TEST_ID_3);
        lenient().when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(TEST_ID_3);

        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptForMedication(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptToCdForTransformedActualProblemHeader(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.ACTUAL_PROBLEM_CODE);
        when(codeableConceptCdMapper.mapToNullFlavorCodeableConcept(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(any(CodeableConcept.class),
            any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(any(CodeableConcept.class),
            any(AllergyIntolerance.AllergyIntoleranceClinicalStatus.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.getDisplayFromCodeableConcept(any(CodeableConcept.class)))
            .thenCallRealMethod();
        when(codeableConceptCdMapper.mapCodeableConceptToCdForBloodPressure(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForTopic(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForTopic(any(CodeableConcept.class), any(String.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForTopic(any(String.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.getCdForTopic())
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.mapToCdForCategory(any(String.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        when(codeableConceptCdMapper.getCdForCategory())
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);


        messageContext = new MessageContext(randomIdGeneratorService);

        ParticipantMapper participantMapper = new ParticipantMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper,
            participantMapper, randomIdGeneratorService, confidentialityService);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper,
            randomIdGeneratorService, confidentialityService);
        DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper
            = new DocumentReferenceToNarrativeStatementMapper(
                messageContext, new SupportedContentTypes(), participantMapper, confidentialityService);

        EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper, confidentialityService),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                codeableConceptCdMapper, new ParticipantMapper()),
            new ConditionLinkSetMapper(
                messageContext, randomIdGeneratorService, codeableConceptCdMapper, participantMapper, confidentialityService),
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            documentReferenceToNarrativeStatementMapper,
            new ImmunizationObservationStatementMapper(
                messageContext,
                codeableConceptCdMapper,
                participantMapper,
                confidentialityService
            ),
            new MedicationStatementMapper(
                messageContext,
                codeableConceptCdMapper,
                participantMapper,
                randomIdGeneratorService,
                confidentialityService
            ),
            new ObservationToNarrativeStatementMapper(messageContext, participantMapper),
            new ObservationStatementMapper(
                messageContext,
                new StructuredObservationValueMapper(),
                new PertinentInformationObservationValueMapper(),
                codeableConceptCdMapper,
                participantMapper
            ),
            new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new DiagnosticReportMapper(
                messageContext, specimenMapper, participantMapper, randomIdGeneratorService, confidentialityService
            ),
            new BloodPressureValidator(),
            codeableConceptCdMapper
        );

        AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(
            messageContext,
            new AgentPersonMapper(messageContext)
        );

        nonConsultationResourceMapper = new NonConsultationResourceMapper(messageContext,
            randomIdGeneratorService,
            encounterComponentsMapper,
            new BloodPressureValidator()
        );

        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService,
            timestampService,
            new EncounterMapper(messageContext, encounterComponentsMapper),
            nonConsultationResourceMapper,
            agentDirectoryMapper,
            messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput(String input, String expected) {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + expected);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + input);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle
        );
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(JSON_INPUT_FILE, EXPECTED_XML_TO_JSON_FILE),
            Arguments.of(FHIR_BUNDLE_WITHOUT_EFFECTIVE_TIME, EXPECTED_XML_WITHOUT_EFFECTIVE_TIME),
            Arguments.of(FHIR_BUNDLE_WITHOUT_HIGH_EFFECTIVE_TIME, EXPECTED_XML_WITHOUT_HIGH_EFFECTIVE_TIME),
            Arguments.of(FHIR_BUNDLE_WITH_EFFECTIVE_TIME, EXPECTED_XML_WITH_EFFECTIVE_TIME),
            Arguments.of(FHIR_BUNDLE_WITH_WITH_OBSERVATIONS_BEFORE_DIAGNOSTIC_REPORT, EXPECTED_XML_WITH_OBSERVATIONS_INSIDE_REPORT),
            Arguments.of(FHIR_BUNDLE_WITH_WITH_OBSERVATIONS_AFTER_DIAGNOSTIC_REPORT, EXPECTED_XML_WITH_OBSERVATIONS_INSIDE_REPORT),
            Arguments.of(FHIR_BUNDLE_WITH_OBSERVATIONS_UNRELATED_TO_DIAGNOSTIC_REPORT, EXPECTED_XML_WITH_STANDALONE_OBSERVATIONS),
            Arguments.of(FHIR_BUNDLE_WITH_OBSERVATIONS_WITH_RELATED_OBSERVATIONS, EXPECTED_XML_WITH_RELATED_OBSERVATIONS)
        );
    }

    @Test
    public void When_MappingJsonBody_Expect_OnlyOneConsultationResource() {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_XML_FOR_ONE_CONSULTATION_RESOURCE);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(ONE_CONSULTATION_RESOURCE_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);
        assertThat(output).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    @Test
    public void When_TransformingResourceToEhrComp_Expect_NoDuplicateMappings() {
        String bundle = ResourceTestFileUtils.getFileContent(DUPLICATE_RESOURCE_BUNDLE);
        Bundle parsedBundle = new FhirParseService().parseResource(bundle, Bundle.class);
        messageContext.initialize(parsedBundle);
        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);
        assertThat(translatedOutput.size()).isEqualTo(1);
    }
}
