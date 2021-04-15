package uk.nhs.adaptors.gp2gp.uat;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.BloodPressureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ConditionLinkSetMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DiaryPlanStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterComponentsMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ImmunizationObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OrganizationToAgentMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PractitionerAgentPersonMapper;
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
    private static final String FILES_PREFIX = "TC4-";
    private static final String INPUT_PATH = "/uat/input/";
    private static final String OUTPUT_PATH = "/uat/output/";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;
    @Mock
    private OrganizationToAgentMapper organizationToAgentMapper;

    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;
    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @BeforeEach
    public void setUp() {
        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber("1234567890")
            .conversationId("test-conversation-id")
            .requestId("test-request-id")
            .fromOdsCode("test-from-ods-code")
            .toOdsCode("test-to-ods-code")
            .build();

        when(randomIdGeneratorService.createNewId()).thenReturn("test-id-1", "test-id-2", "test-id-3");
        when(timestampService.now()).thenReturn(Instant.parse("2020-01-01T01:01:01.01Z"));

        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);
        messageContext = new MessageContext(randomIdGeneratorService);

        final CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        final ParticipantMapper participantMapper = new ParticipantMapper();

        final EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(messageContext,
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper),
            new ObservationToNarrativeStatementMapper(messageContext, participantMapper),
            new ObservationStatementMapper(messageContext, new StructuredObservationValueMapper(),
                new PertinentInformationObservationValueMapper(), codeableConceptCdMapper, participantMapper),
            new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new ConditionLinkSetMapper(messageContext, randomIdGeneratorService, codeableConceptCdMapper),
            new BloodPressureMapper(messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                codeableConceptCdMapper));

        final PractitionerAgentPersonMapper practitionerAgentPersonMapper = new PractitionerAgentPersonMapper(messageContext,
            new OrganizationToAgentMapper(messageContext));

        when(organizationToAgentMapper.mapOrganizationToAgent(any(Organization.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        final AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(practitionerAgentPersonMapper,
            organizationToAgentMapper);

        final EncounterMapper encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);

        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService, timestampService, encounterMapper, agentDirectoryMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutput(String inputJson, String expectedOutputXml) throws IOException {
        final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + FILES_PREFIX + expectedOutputXml);
        final String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + FILES_PREFIX + inputJson);
        final Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);

        messageContext.initialize(bundle);

        final EhrExtractTemplateParameters ehrExtractTemplateParameters =
            ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

        final String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        final String hl7TranslatedResponse = outputMessageWrapperMapper.map(getGpcStructuredTaskDefinition, ehrExtractContent);

        assertThat(hl7TranslatedResponse).isEqualToIgnoringWhitespace(expectedJsonToXmlContent);
    }

    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(Arguments.of("9465701483_Dougill_full_20210119.json", "9465701483_Dougill_full_20210119.xml"),
            Arguments.of("9465701483_Nel_full_20210119.json", "9465701483_Nel_full_20210119.xml"),
            Arguments.of("9465701459_Nel_full_20210119.json", "9465701459_Nel_full_20210119.xml"),
            Arguments.of("9465698679_Gainsford_full_20210119.json", "9465698679_Gainsford_full_20210119.xml"),
            Arguments.of("9465700193_Birdi_full_20210119.json", "9465700193_Birdi_full_20210119.xml"),
            Arguments.of("9465701262_Meyers_full_20210119.json", "9465701262_Meyers_full_20210119.xml"),
//            Arguments.of("9465699918_Magre_full_20210119.json", "9465699918_Magre_full_20210119.xml"),
//            Arguments.of("9465701297_Livermore_full_20210119.json", "9465701297_Livermore_full_20210119.xml"),
            Arguments.of("9465700339_Yamura_full_20210119.json", "9465700339_Yamura_full_20210119.xml"),
            Arguments.of("9465701718_Guerra_full_20210119.json", "9465701718_Guerra_full_20210119.xml"),
            Arguments.of("9465699926_Sajal_full_20210122.json", "9465699926_Sajal_full_20210122.xml"),
            Arguments.of("9465700088_Mold_full_20210119.json", "9465700088_Mold_full_20210119.xml"),
            Arguments.of("9465698490_Daniels_full_20210119.json", "9465698490_Daniels_full_20210119.xml"));
    }
}
