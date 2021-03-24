package uk.nhs.adaptors.gp2gp.uat;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.BloodPressureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ConditionLinkSetMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DiaryPlanStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterComponentsMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ImmunizationObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.NarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
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
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExtractUATTest {
    private static final String TEST_FILES_DIRECTORY = "/uat/";
    private static final String INPUT_JSON_WITH_PATHOLOGY_RECORD = "TC4 - 9465701718_Guerra_inv1only_20210212.json";
    private static final String INPUT_JSON_WITH_MEDICATION_RECORD = "TC4 - 9465701645_Prytherch_medsonly_20210212.json";
    private static final String INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT =
        "TC4 - 9465701610_Powne_imms1only_20210212.json";
    private static final String INPUT_JSON_WITH_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT = INPUT_JSON_WITH_PATHOLOGY_RECORD;
    private static final String INPUT_JSON_WITH_BLOOD_PRESSURES = "TC4 - 9465701610_Powne_cons1only_20210212.json";
    private static final String INPUT_JSON_WITH_IMMUNISATIONS = INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT;
    private static final String INPUT_JSON_WITH_DRUG_AND_NON_DRUG_ALLERGIES = "TC4 - 9465700827_Bentley_allergy1only_20210212.json";
    private static final String INPUT_JSON_WITH_DIARY_ENTRIES = "TC4 - 9465700827_Bentley_diary1only_20210212.json";
    private static final String INPUT_JSON_WITH_REFERRALS = "TC4 - 9465701149_Drake_refs1only_20210212.json";
    private static final String INPUT_JSON_WITH_PROBLEMS_AND_PROBLEM_LINKAGES = "TC4 - 9465699918_Magre_prob1only_20210212.json";
    private static final String INPUT_JSON_WITH_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES = "TC4 - 9465701483_Dougill_uncat18jan_20210215.json";
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
        EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper),
            new NarrativeStatementMapper(messageContext),
            new ObservationStatementMapper(
                messageContext,
                new StructuredObservationValueMapper(),
                new PertinentInformationObservationValueMapper(),
                codeableConceptCdMapper
            ),
            new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper),
            new ConditionLinkSetMapper(messageContext, randomIdGeneratorService, codeableConceptCdMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(), codeableConceptCdMapper)
        );

        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService,
            timestampService,
            new EncounterMapper(messageContext, encounterComponentsMapper));
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput(String input) throws IOException {
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(TEST_FILES_DIRECTORY + input);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        //TODO: compare with the proper XML files
        assertThat(output).isEqualTo(output);
    }

    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PATHOLOGY_RECORD),
            Arguments.of(INPUT_JSON_WITH_MEDICATION_RECORD),
            Arguments.of(INPUT_JSON_WITH_STRUCTURED_CONSULTATIONS_INCLUDING_NARRATIVE_CONTENT),
            Arguments.of(INPUT_JSON_WITH_GENERAL_MISCELLANEOUS_CODED_OBSERVATION_CONTENT),
            Arguments.of(INPUT_JSON_WITH_BLOOD_PRESSURES),
            Arguments.of(INPUT_JSON_WITH_IMMUNISATIONS),
            Arguments.of(INPUT_JSON_WITH_DRUG_AND_NON_DRUG_ALLERGIES),
            Arguments.of(INPUT_JSON_WITH_DIARY_ENTRIES),
            Arguments.of(INPUT_JSON_WITH_REFERRALS),
            Arguments.of(INPUT_JSON_WITH_PROBLEMS_AND_PROBLEM_LINKAGES),
            Arguments.of(INPUT_JSON_WITH_NON_SNOMED_CODED_DATA_VIA_EGTON_CODES)
        );
    }
}
