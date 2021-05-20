package uk.nhs.adaptors.gp2gp.uat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.RandomIdGeneratorServiceStub;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentPersonMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EhrExtractUATTest {
    private static final String FILES_PREFIX = "TC4-";
    private static final String INPUT_PATH = "/uat/input/";
    private static final String OUTPUT_PATH = "/uat/output/";

    @Mock
    private TimestampService timestampService;

    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;
    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @BeforeEach
    public void setUp() {
        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .nhsNumber("1234567890")
            .conversationId("6910A49D-1F97-4AA0-9C69-197EE9464C76")
            .requestId("17A3A644-A4EB-4C0A-A870-152D310FD1F8")
            .fromOdsCode("GP2GPTEST")
            .toOdsCode("GP2GPTEST")
            .toAsid("GP2GPTEST")
            .fromAsid("GP2GPTEST")
            .build();

        final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorServiceStub();
        when(timestampService.now()).thenReturn(Instant.parse("2020-01-01T01:01:01.01Z"));

        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);
        messageContext = new MessageContext(randomIdGeneratorService);

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();
        ParticipantMapper participantMapper = new ParticipantMapper();
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper, randomIdGeneratorService);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper);

        final EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                codeableConceptCdMapper, new ParticipantMapper()),
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
            new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new DiagnosticReportMapper(messageContext, specimenMapper, participantMapper, randomIdGeneratorService)
        );

        AgentPersonMapper agentPersonMapper
            = new AgentPersonMapper(messageContext);
        final AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(messageContext, agentPersonMapper);

        final EncounterMapper encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);

        final NonConsultationResourceMapper nonConsultationResourceMapper =
            new NonConsultationResourceMapper(messageContext, randomIdGeneratorService, encounterComponentsMapper);
        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService, timestampService, encounterMapper,
            nonConsultationResourceMapper, agentDirectoryMapper, messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutput(String inputJson, String expectedOutputXml) throws IOException {
        final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(OUTPUT_PATH + FILES_PREFIX + expectedOutputXml);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + FILES_PREFIX + inputJson);
        inputJsonFileContent = removeEmptyDescriptions(inputJsonFileContent);
        final Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);

        messageContext.initialize(bundle);

        final EhrExtractTemplateParameters ehrExtractTemplateParameters =
            ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

        final String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        final String hl7TranslatedResponse = outputMessageWrapperMapper.map(getGpcStructuredTaskDefinition, ehrExtractContent);

        assertThat(hl7TranslatedResponse).isEqualTo(expectedJsonToXmlContent);
    }

    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(
            Arguments.of("9465701483_Dougill_full_20210119.json", "9465701483_Dougill_full_20210119.xml"),
            Arguments.of("9465701483_Nel_full_20210119.json", "9465701483_Nel_full_20210119.xml"),
            Arguments.of("9465701459_Nel_full_20210119.json", "9465701459_Nel_full_20210119.xml"),
            Arguments.of("9465698679_Gainsford_full_20210119.json", "9465698679_Gainsford_full_20210119.xml"),
            Arguments.of("9465700193_Birdi_full_20210119.json", "9465700193_Birdi_full_20210119.xml"),
            Arguments.of("9465701262_Meyers_full_20210119.json", "9465701262_Meyers_full_20210119.xml"),
            Arguments.of("9465699918_Magre_full_20210119.json", "9465699918_Magre_full_20210119.xml"),
            Arguments.of("9465701297_Livermore_full_20210119.json", "9465701297_Livermore_full_20210119.xml"),
            Arguments.of("9465700339_Yamura_full_20210119.json", "9465700339_Yamura_full_20210119.xml"),
            Arguments.of("9465699926_Sajal_full_20210122.json", "9465699926_Sajal_full_20210122.xml"),
            Arguments.of("9465698490_Daniels_full_20210119.json", "9465698490_Daniels_full_20210119.xml")
        );
    }

    // TODO, workaround until NIAD-1342 is fixed
    private String removeEmptyDescriptions(String json) {
        String emptyDescriptionElement = "\"description\": \"\"";
        return json.lines().filter(l -> !l.contains(emptyDescriptionElement)).collect(Collectors.joining());
    }
}
