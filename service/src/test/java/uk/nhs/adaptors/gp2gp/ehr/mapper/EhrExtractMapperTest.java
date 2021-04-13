package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EhrExtractMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String OUTPUT_DIRECTORY = "output/";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String OUTPUT_PATH = TEST_FILE_DIRECTORY + OUTPUT_DIRECTORY;
    private static final String JSON_INPUT_FILE = "gpc-access-structured.json";
    private static final String EXPECTED_XML_TO_JSON_FILE = "ExpectedEhrExtractResponseFromJson.xml";
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
    private OrganizationToAgentMapper organizationToAgentMapper;

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
        when(organizationToAgentMapper.mapOrganizationToAgent(any(Organization.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        messageContext = new MessageContext(randomIdGeneratorService);

        ParticipantMapper participantMapper = new ParticipantMapper();
        EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper),
            new ObservationToNarrativeStatementMapper(messageContext, participantMapper),
            new ObservationStatementMapper(
                messageContext,
                new StructuredObservationValueMapper(),
                new PertinentInformationObservationValueMapper(),
                codeableConceptCdMapper,
                participantMapper
            ),
            new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper),
            new ConditionLinkSetMapper(messageContext, randomIdGeneratorService, codeableConceptCdMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(), codeableConceptCdMapper)
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
            agentDirectoryMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingProperJsonRequestBody_Expect_ProperXmlOutput() throws IOException {
        String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + EXPECTED_XML_TO_JSON_FILE);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + JSON_INPUT_FILE);
        Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);
        messageContext.initialize(bundle);

        EhrExtractTemplateParameters ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
            getGpcStructuredTaskDefinition,
            bundle);
        String output = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);
        assertThat(output).isEqualTo(expectedJsonToXmlContent);
    }
}
