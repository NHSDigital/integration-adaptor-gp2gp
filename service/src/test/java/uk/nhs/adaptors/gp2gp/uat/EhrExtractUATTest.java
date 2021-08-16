package uk.nhs.adaptors.gp2gp.uat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.XsdValidator.validateFileContentAgainstSchema;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.RandomIdGeneratorServiceStub;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectoryMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentPersonMapper;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.MultiStatementObservationHolderFactory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import wiremock.org.custommonkey.xmlunit.XMLAssert;

@ExtendWith(MockitoExtension.class)
public class EhrExtractUATTest {
    private static final String FILES_PREFIX = "TC4-";
    private static final String INPUT_PATH = "/uat/input/";
    private static final String OUTPUT_PATH = "/uat/output/";
    private static final boolean OVERWRITE_XML = false;

    @Mock
    private TimestampService timestampService;

    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;
    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @SuppressWarnings("unused")
    private static Stream<Arguments> testValueFilePaths() {
        return Stream.of(
            Arguments.of("9465701483_Dougill_full_20210119.json", "9465701483_Dougill_full_20210119.xml"),
            Arguments.of("9465701483_Nel_full_20210119.json", "9465701483_Nel_full_20210119.xml"),
            Arguments.of("9465698679_Gainsford_full_20210119.json", "9465698679_Gainsford_full_20210119.xml"),
            Arguments.of("9465700193_Birdi_full_20210119.json", "9465700193_Birdi_full_20210119.xml"),
            Arguments.of("9465701262_Meyers_full_20210119.json", "9465701262_Meyers_full_20210119.xml"),
            Arguments.of("9465699918_Magre_full_20210119.json", "9465699918_Magre_full_20210119.xml"),
            Arguments.of("9465701297_Livermore_full_20210119.json", "9465701297_Livermore_full_20210119.xml"),
            Arguments.of("9465700339_Yamura_full_20210119.json", "9465700339_Yamura_full_20210119.xml"),
            Arguments.of("9465699926_Sajal_full_20210122.json", "9465699926_Sajal_full_20210122.xml"),
            Arguments.of("9465698490_Daniels_full_20210119.json", "9465698490_Daniels_full_20210119.xml"),
            Arguments.of("9465701718_Guerra_full_20210119.json", "9465701718_Guerra_full_20210119.xml"),
            Arguments.of("9465700088_Mold_full_20210119.json", "9465700088_Mold_full_20210119.xml")
        );
    }

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
        MultiStatementObservationHolderFactory multiStatementObservationHolderFactory =
            new MultiStatementObservationHolderFactory(messageContext, randomIdGeneratorService);
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper,
            multiStatementObservationHolderFactory);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper, randomIdGeneratorService);
        DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper
            = new DocumentReferenceToNarrativeStatementMapper(messageContext, new SupportedContentTypes());

        final EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
            messageContext,
            new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper),
            new BloodPressureMapper(
                messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                codeableConceptCdMapper, new ParticipantMapper()),
            new ConditionLinkSetMapper(
                messageContext, randomIdGeneratorService, codeableConceptCdMapper, participantMapper),
            new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
            documentReferenceToNarrativeStatementMapper,
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
            new NonConsultationResourceMapper(messageContext, randomIdGeneratorService, encounterComponentsMapper,
                documentReferenceToNarrativeStatementMapper);
        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService, timestampService, encounterMapper,
            nonConsultationResourceMapper, agentDirectoryMapper, messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testValueFilePaths")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutput(String inputJson, String expectedOutputXml)
        throws IOException, SAXException {
        final String expectedXmlResourcePath = OUTPUT_PATH + FILES_PREFIX + expectedOutputXml;
        final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(expectedXmlResourcePath);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + FILES_PREFIX + inputJson);
        inputJsonFileContent = removeEmptyDescriptions(inputJsonFileContent);
        final Bundle bundle = new FhirParseService().parseResource(inputJsonFileContent, Bundle.class);

        messageContext.initialize(bundle);

        final EhrExtractTemplateParameters ehrExtractTemplateParameters =
            ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

        final String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        final String hl7TranslatedResponse = outputMessageWrapperMapper.map(getGpcStructuredTaskDefinition, ehrExtractContent);

        if (OVERWRITE_XML) {
            try (PrintWriter printWriter = new PrintWriter("src/test/resources" + expectedXmlResourcePath, StandardCharsets.UTF_8)) {
                printWriter.print(hl7TranslatedResponse);
            }
            fail("Re-run the tests with OVERWRITE_XML=false");
        }

        XMLAssert.assertXMLEqual(hl7TranslatedResponse, expectedJsonToXmlContent);

        assertThatCode(() -> validateFileContentAgainstSchema(hl7TranslatedResponse))
            .doesNotThrowAnyException();

        assertThatAgentReferencesAreValid(hl7TranslatedResponse);
    }

    // TODO, workaround until NIAD-1342 is fixed
    private String removeEmptyDescriptions(String json) {
        String emptyDescriptionElement = "\"description\": \"\"";
        return json.lines().filter(l -> !l.contains(emptyDescriptionElement)).collect(Collectors.joining());
    }

    @SneakyThrows
    private void assertThatAgentReferencesAreValid(String hl7) {
        var xPathService = new XPathService();
        var document = xPathService.parseDocumentFromXml(hl7);
        var agentRefIdNodes = xPathService.getNodes(document, "//agentRef/id");
        Set<String> referencedAgentIds = extractIdsFromNodeList(agentRefIdNodes, true);
        var agentIdNodes = xPathService.getNodes(document, "//Agent/id");
        Set<String> agentIds = extractIdsFromNodeList(agentIdNodes, false);

        assertThat(referencedAgentIds).isNotEmpty()
            .allMatch(this::isUuid, "All referenced ids must be UUIDs");
        assertThat(agentIds)
            .isNotEmpty()
            .containsAll(referencedAgentIds);
    }

    private Set<String> extractIdsFromNodeList(NodeList nodeList, boolean allowSkipNullFlavour) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            var agentIdNode = nodeList.item(i);
            if (allowSkipNullFlavour && agentIdNode.getAttributes().getNamedItem("nullFlavor") != null) {
                continue;
            }

            assertThat(agentIdNode.hasAttributes())
                .withFailMessage("Node %s has no attributes", agentIdNode)
                .isTrue();
            assertThat(agentIdNode.getAttributes().getNamedItem("root"))
                .withFailMessage("Node %s is missing attribute 'root'", agentIdNode)
                .isNotNull();

            var id = agentIdNode.getAttributes().getNamedItem("root").getNodeValue();
            ids.add(id);
        }
        return ids;
    }

    private boolean isUuid(String id) {
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
