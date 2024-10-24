package uk.nhs.adaptors.gp2gp.uat;

import lombok.SneakyThrows;
import org.hl7.fhir.dstu3.model.Bundle;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.nhs.adaptors.gp2gp.RandomIdGeneratorServiceStub;
import uk.nhs.adaptors.gp2gp.common.configuration.RedactionsContext;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.BloodPressureValidator;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import wiremock.org.custommonkey.xmlunit.XMLAssert;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.XsdValidator.validateFileContentAgainstSchema;

@ExtendWith(MockitoExtension.class)
@Disabled
public class EhrExtractUATTest {
    private static final String INPUT_PATH = "/uat/input/";
    private static final String OUTPUT_PATH = "/uat/output/";
    private static final boolean OVERWRITE_XML = false;

    @Mock
    private TimestampService timestampService;
    @Mock
    private ConfidentialityService confidentialityService;
    @Mock
    private RedactionsContext redactionsContext;

    private EhrExtractMapper ehrExtractMapper;
    private MessageContext messageContext;

    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

    @SuppressWarnings("unused")
    private static Stream<Arguments> testValueFilePathsTC4() {
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

    private static Stream<Arguments> testValueFilePathsTC7() {
        return Stream.of(
            Arguments.of("9465698490_Daniels_full_20210602.json", "9465698490_Daniels_full_20210602.xml"),
            Arguments.of("9465698679_Gainsford_full_20210602.json", "9465698679_Gainsford_full_20210602.xml"),
            Arguments.of("9465699918_Magre_full_20210602.json", "9465699918_Magre_full_20210602.xml"),
            Arguments.of("9465700088_Mold_full_20210602.json", "9465700088_Mold_full_20210602.xml"),
            Arguments.of("9465700193_Birdi_full_20210602.json", "9465700193_Birdi_full_20210602.xml"),
            Arguments.of("9465700339_Yamura_full_20210602.json", "9465700339_Yamura_full_20210602.xml"),
            Arguments.of("9465701262_Meyers_full_20210602.json", "9465701262_Meyers_full_20210602.xml"),
            Arguments.of("9465701297_Livermore_full_20210602.json", "9465701297_Livermore_full_20210602.xml"),
            Arguments.of("9465701459_Nel_full_20210602.json", "9465701459_Nel_full_20210602.xml"),
            Arguments.of("9465701483_Dougill_full_20210602.json", "9465701483_Dougill_full_20210602.xml"),
            Arguments.of("9465701718_Guerra_full_20210602.json", "9465701718_Guerra_full_20210602.xml")
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

        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService, redactionsContext);
        messageContext = new MessageContext(randomIdGeneratorService);

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        final EncounterComponentsMapper encounterComponentsMapper =
            getEncounterComponentsMapper(randomIdGeneratorService, codeableConceptCdMapper);

        AgentPersonMapper agentPersonMapper
            = new AgentPersonMapper(messageContext);
        final AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(messageContext, agentPersonMapper);

        final EncounterMapper encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);

        final NonConsultationResourceMapper nonConsultationResourceMapper =
            new NonConsultationResourceMapper(messageContext, randomIdGeneratorService, encounterComponentsMapper,
                new BloodPressureValidator());
        ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService, timestampService, encounterMapper,
            nonConsultationResourceMapper, agentDirectoryMapper, messageContext);
        lenient().when(confidentialityService.generateConfidentialityCode(any()))
            .thenReturn(Optional.empty());
    }

    private @NotNull EncounterComponentsMapper getEncounterComponentsMapper(RandomIdGeneratorService randomIdGeneratorService,
                                                                            CodeableConceptCdMapper codeableConceptCdMapper) {
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();
        ParticipantMapper participantMapper = new ParticipantMapper();
        ObservationMapper specimenObservationMapper = new ObservationMapper(
            messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper,
            randomIdGeneratorService, confidentialityService);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper,
            randomIdGeneratorService, confidentialityService);
        DocumentReferenceToNarrativeStatementMapper documentReferenceToNarrativeStatementMapper
            = new DocumentReferenceToNarrativeStatementMapper(
                messageContext, new SupportedContentTypes(), participantMapper, confidentialityService);

        return new EncounterComponentsMapper(
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
            new DiagnosticReportMapper(messageContext, specimenMapper, participantMapper, randomIdGeneratorService, confidentialityService),
            new BloodPressureValidator(),
            codeableConceptCdMapper
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testValueFilePathsTC4")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutputTC4(String inputJson, String expectedOutputXml)
        throws IOException, SAXException {
        final String expectedXmlResourcePath = OUTPUT_PATH + "TC4/" + expectedOutputXml;
        final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(expectedXmlResourcePath);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + "TC4/" + inputJson);
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

    @ParameterizedTest
    @MethodSource("testValueFilePathsTC7")
    public void When_MappingValidJsonRequestBody_Expect_ValidXmlOutputTC7(String inputJson, String expectedOutputXml)
        throws IOException, SAXException {
        final String expectedXmlResourcePath = OUTPUT_PATH + "TC7/" + expectedOutputXml;
        final String expectedJsonToXmlContent = ResourceTestFileUtils.getFileContent(expectedXmlResourcePath);
        String inputJsonFileContent = ResourceTestFileUtils.getFileContent(INPUT_PATH + "TC7/" + inputJson);
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
