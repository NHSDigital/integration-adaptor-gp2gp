package uk.nhs.adaptors.gp2gp.uat;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectoryMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.BloodPressureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ConditionLinkSetMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DiaryPlanStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DocumentReferenceToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterComponentsMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ImmunizationObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.NonConsultationResourceMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OrganizationToAgentMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PractitionerAgentPersonMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EhrExtractUATTest {
    private static final String TEST_FILE_DIRECTORY = "/uat/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String FILES_PREFIX = "TC4-";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String INPUT_JSON_WITH_PATHOLOGY_RECORD = "9465701718_Guerra_inv1only_20210212.json";
    private static final String INPUT_JSON_WITH_MEDICATION_RECORD = "9465701645_Prytherch_medsonly_20210212.json";
    private static final String INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT =
        "9465701610_Powne_imms1only_20210212.json";
    private static final String INPUT_JSON_WITH_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT = INPUT_JSON_WITH_PATHOLOGY_RECORD;
    private static final String INPUT_JSON_WITH_BLOOD_PRESSURES = "9465701610_Powne_cons1only_20210212.json";
    private static final String INPUT_JSON_WITH_IMMUNISATIONS = INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT;
    private static final String INPUT_JSON_WITH_DRUG_AND_NON_DRUG_ALLERGIES = "9465700827_Bentley_allergy1only_20210212.json";
    private static final String INPUT_JSON_WITH_DIARY_ENTRIES = "9465700827_Bentley_diary1only_20210212.json";
    private static final String INPUT_JSON_WITH_REFERRALS = "9465701149_Drake_refs1only_20210212.json";
    private static final String INPUT_JSON_WITH_PROBLEMS_AND_PROBLEM_LINKAGES = "9465699918_Magre_prob1only_20210212.json";
    private static final String INPUT_JSON_WITH_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES = "9465701483_Dougill_uncat18jan_20210215.json";
    private static final String OUTPUT_XML_USES_PATHOLOGY_RECORD = "9465701718_Guerra_inv1only_20210212.xml";
    private static final String OUTPUT_XML_USES_MEDICATION_RECORD = "9465701645_Prytherch_medsonly_20210212.xml";
    private static final String OUTPUT_XML_USES_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT =
        "9465701610_Powne_imms1only_20210212.xml";
    private static final String OUTPUT_XML_USES_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT = OUTPUT_XML_USES_PATHOLOGY_RECORD;
    private static final String OUTPUT_XML_USES_BLOOD_PRESSURES = "9465701610_Powne_cons1only_20210212.xml";
    private static final String OUTPUT_XML_USES_IMMUNISATIONS = OUTPUT_XML_USES_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT;
    private static final String OUTPUT_XML_USES_DRUG_AND_NON_DRUG_ALLERGIES = "9465700827_Bentley_allergy1only_20210212.xml";
    private static final String OUTPUT_XML_USES_DIARY_ENTRIES = "9465700827_Bentley_diary1only_20210212.xml";
    private static final String OUTPUT_XML_USES_REFERRALS = "9465701149_Drake_refs1only_20210212.xml";
    private static final String OUTPUT_XML_USES_PROBLEMS_AND_PROBLEM_LINKAGES = "9465699918_Magre_prob1only_20210212.xml";
    private static final String OUTPUT_XML_USES_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES = "9465701483_Dougill_uncat18jan_20210215.xml";
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
    private OrganizationToAgentMapper organizationToAgentMapper;

    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;
    private OutputMessageWrapperMapper outputMessageWrapperMapper;

    @BeforeEach
    public void setUp() {
        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber(TEST_NHS_NUMBER)
            .conversationId(TEST_CONVERSATION_ID)
            .requestId(TEST_REQUEST_ID)
            .fromOdsCode(TEST_FROM_ODS_CODE)
            .toOdsCode(TEST_TO_ODS_CODE)
            .build();

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        ParticipantMapper participantMapper = new ParticipantMapper();

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID_1, TEST_ID_2, TEST_ID_3);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        when(organizationToAgentMapper.mapOrganizationToAgent(any(Organization.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);
        messageContext = new MessageContext(randomIdGeneratorService);
        EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(), codeableConceptCdMapper),
            new ConditionLinkSetMapper(
                messageContext, randomIdGeneratorService, codeableConceptCdMapper, participantMapper),
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper),
            new DocumentReferenceToNarrativeStatementMapper(messageContext),
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
            new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper)
        );

        AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(
            new PractitionerAgentPersonMapper(
                messageContext,
                new OrganizationToAgentMapper(messageContext)
            ),
            organizationToAgentMapper
        );

        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService,
            timestampService,
            new EncounterMapper(messageContext, encounterComponentsMapper),
            new NonConsultationResourceMapper(messageContext, randomIdGeneratorService, encounterComponentsMapper),
            agentDirectoryMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Disabled("These tests are being fixed in NIAD-1288")
    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutput(String inputJson, String expectedOutputXml) throws IOException {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + FILES_PREFIX + expectedOutputXml);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + FILES_PREFIX + inputJson);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);
        var hl7TranslatedResponse = outputMessageWrapperMapper.map(
            getGpcStructuredTaskDefinition,
            ehrExtractContent);

        assertThat(hl7TranslatedResponse).isEqualTo(expectedJsonToXmlContent);
    }

    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PATHOLOGY_RECORD, OUTPUT_XML_USES_PATHOLOGY_RECORD),
            Arguments.of(INPUT_JSON_WITH_MEDICATION_RECORD, OUTPUT_XML_USES_MEDICATION_RECORD),
            Arguments.of(INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT,
                OUTPUT_XML_USES_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT),
            Arguments.of(INPUT_JSON_WITH_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT,
                OUTPUT_XML_USES_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT),
            Arguments.of(INPUT_JSON_WITH_BLOOD_PRESSURES, OUTPUT_XML_USES_BLOOD_PRESSURES),
            Arguments.of(INPUT_JSON_WITH_IMMUNISATIONS, OUTPUT_XML_USES_IMMUNISATIONS),
            Arguments.of(INPUT_JSON_WITH_DRUG_AND_NON_DRUG_ALLERGIES, OUTPUT_XML_USES_DRUG_AND_NON_DRUG_ALLERGIES),
            Arguments.of(INPUT_JSON_WITH_DIARY_ENTRIES, OUTPUT_XML_USES_DIARY_ENTRIES),
            Arguments.of(INPUT_JSON_WITH_REFERRALS, OUTPUT_XML_USES_REFERRALS),
            Arguments.of(INPUT_JSON_WITH_PROBLEMS_AND_PROBLEM_LINKAGES, OUTPUT_XML_USES_PROBLEMS_AND_PROBLEM_LINKAGES),
            Arguments.of(INPUT_JSON_WITH_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES, OUTPUT_XML_USES_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES)
        );
    }
}
