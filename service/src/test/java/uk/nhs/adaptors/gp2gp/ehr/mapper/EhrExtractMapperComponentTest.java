package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

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

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

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
    private static final String EXPECTED_XML_FOR_ONE_CONSULTATION_RESOURCE = "ExpectedResponseFrom1ConsultationResponse.xml";
    private static final String FHIR_BUNDLE_WITHOUT_EFFECTIVE_TIME = "fhir-bundle-without-effective-time.json";
    private static final String FHIR_BUNDLE_WITHOUT_HIGH_EFFECTIVE_TIME = "fhir-bundle-without-high-effective-time.json";
    private static final String FHIR_BUNDLE_WITH_EFFECTIVE_TIME = "fhir-bundle-with-effective-time.json";
    private static final String EXPECTED_XML_TO_JSON_FILE = "expected-ehr-extract-response-from-json.xml";
    private static final String EXPECTED_XML_WITHOUT_EFFECTIVE_TIME = "expected-xml-without-effective-time.xml";
    private static final String EXPECTED_XML_WITHOUT_HIGH_EFFECTIVE_TIME = "expected-xml-without-high-effective-time.xml";
    private static final String EXPECTED_XML_WITH_EFFECTIVE_TIME = "expected-xml-with-effective-time.xml";
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
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);

        ParticipantMapper participantMapper = new ParticipantMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper, randomIdGeneratorService);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper, randomIdGeneratorService);

        EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                codeableConceptCdMapper, new ParticipantMapper()),
            new ConditionLinkSetMapper(
                messageContext, randomIdGeneratorService, codeableConceptCdMapper, participantMapper),
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new DocumentReferenceToNarrativeStatementMapper(messageContext, new SupportedContentTypes()),
            new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new MedicationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper, randomIdGeneratorService),
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
                messageContext, specimenMapper, participantMapper, randomIdGeneratorService
            )
        );

        AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(
            messageContext,
            new AgentPersonMapper(messageContext)
        );

        nonConsultationResourceMapper = new NonConsultationResourceMapper(messageContext,
            randomIdGeneratorService,
            encounterComponentsMapper);

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
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput(String input, String expected) throws IOException {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + expected);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + input);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);
        assertThat(output).isEqualTo(expectedJsonToXmlContent);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(JSON_INPUT_FILE, EXPECTED_XML_TO_JSON_FILE),
            Arguments.of(FHIR_BUNDLE_WITHOUT_EFFECTIVE_TIME, EXPECTED_XML_WITHOUT_EFFECTIVE_TIME),
            Arguments.of(FHIR_BUNDLE_WITHOUT_HIGH_EFFECTIVE_TIME, EXPECTED_XML_WITHOUT_HIGH_EFFECTIVE_TIME),
            Arguments.of(FHIR_BUNDLE_WITH_EFFECTIVE_TIME, EXPECTED_XML_WITH_EFFECTIVE_TIME)
        );
    }

    @Test
    public void When_MappingJsonBody_Expect_OnlyOneConsultationResource() throws IOException {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_XML_FOR_ONE_CONSULTATION_RESOURCE);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(ONE_CONSULTATION_RESOURCE_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);
        assertThat(output).isEqualTo(expectedJsonToXmlContent);
    }

    @Test
    public void When_TransformingResourceToEhrComp_Expect_NoDuplicateMappings() throws IOException {
        String bundle = ResourceTestFileUtils.getFileContent(DUPLICATE_RESOURCE_BUNDLE);
        Bundle parsedBundle = new FhirParseService().parseResource(bundle, Bundle.class);
        messageContext.initialize(parsedBundle);
        var translatedOutput = nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(parsedBundle);
        assertThat(translatedOutput.size()).isEqualTo(1);
    }
}
