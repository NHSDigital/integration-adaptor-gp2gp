package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
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
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DiagnosticReportMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";

    private static final String INPUT_JSON_BUNDLE = "fhir_bundle.json";

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";

    private static final String INPUT_JSON_REQUIRED_DATA = "diagnostic-report-with-required-data.json";
    private static final String INPUT_JSON_EMPTY_SPECIMENS = "diagnostic-report-with-empty-specimens.json";
    private static final String INPUT_JSON_EMPTY_RESULTS = "diagnostic-report-with-empty-results.json";
    private static final String INPUT_JSON_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.json";
    private static final String INPUT_JSON_ONE_RESULT = "diagnostic-report-with-one-result.json";
    private static final String INPUT_JSON_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.json";
    private static final String INPUT_JSON_MULTI_RESULTS = "diagnostic-report-with-multi-results.json";
    private static final String INPUT_JSON_PERFORMER = "diagnostic-report-with-performer.json";
    private static final String INPUT_JSON_PERFORMER_NO_ACTOR = "diagnostic-report-with-performer-no-actor.json";
    private static final String INPUT_JSON_CONCLUSION = "diagnostic-report-with-conclusion.json";
    private static final String INPUT_JSON_CODED_DIAGNOSIS = "diagnostic-report-with-coded-diagnosis.json";
    private static final String INPUT_JSON_MULTIPLE_CODED_DIAGNOSIS = "diagnostic-report-with-multiple-coded-diagnosis.json";

    private static final String OUTPUT_XML_REQUIRED_DATA = "diagnostic-report-with-required-data.xml";
    private static final String OUTPUT_XML_DUMMY_NARRATIVE = "diagnostic-report-with-dummy-narrative.xml";
    private static final String OUTPUT_XML_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.xml";
    private static final String OUTPUT_XML_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.xml";
    private static final String OUTPUT_XML_PARTICIPANT = "diagnostic-report-with-participant.xml";
    private static final String OUTPUT_XML_CONCLUSION = "diagnostic-report-with-conclusion.xml";
    private static final String OUTPUT_XML_CODED_DIAGNOSIS = "diagnostic-report-with-coded-diagnosis.xml";
    private static final String OUTPUT_XML_MULTIPLE_CODED_DIAGNOSIS = "diagnostic-report-with-multiple-coded-diagnosis.xml";

    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    @Mock
    private SpecimenMapper specimenMapper;
    @Mock
    private MessageContext messageContext;
    @Mock
    private IdMapper idMapper;
    @Mock
    private AgentDirectory agentDirectory;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private DiagnosticReportMapper mapper;

    @BeforeEach
    public void setUp() throws IOException {
        final Bundle bundle = new FhirParseService()
            .parseResource(ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + INPUT_JSON_BUNDLE), Bundle.class);

        when(messageContext.getIdMapper()).thenReturn(idMapper);
        when(messageContext.getInputBundleHolder()).thenReturn(new InputBundle(bundle));
        when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenAnswer(mockIdForResourceAndId());
        when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockIdForReference());

        when(specimenMapper.mapSpecimenToCompoundStatement(any(Specimen.class), anyList(), any(DiagnosticReport.class)))
            .thenAnswer(mockSpecimenMapping());

        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        mapper = new DiagnosticReportMapper(messageContext, specimenMapper, new ParticipantMapper(), randomIdGeneratorService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingDiagnosticReportJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        final CharSequence expectedOutputMessage = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + outputXml);
        final String jsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputJson);
        final DiagnosticReport diagnosticReport = new FhirParseService().parseResource(jsonInput, DiagnosticReport.class);

        final String outputMessage = mapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);
        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_REQUIRED_DATA, OUTPUT_XML_DUMMY_NARRATIVE),
            Arguments.of(INPUT_JSON_EMPTY_SPECIMENS, OUTPUT_XML_DUMMY_NARRATIVE),
            Arguments.of(INPUT_JSON_EMPTY_RESULTS, OUTPUT_XML_DUMMY_NARRATIVE),
            Arguments.of(INPUT_JSON_ONE_SPECIMEN, OUTPUT_XML_ONE_SPECIMEN),
            Arguments.of(INPUT_JSON_ONE_RESULT, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_MULTI_SPECIMENS, OUTPUT_XML_MULTI_SPECIMENS),
            Arguments.of(INPUT_JSON_MULTI_RESULTS, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_PERFORMER, OUTPUT_XML_PARTICIPANT),
            Arguments.of(INPUT_JSON_PERFORMER_NO_ACTOR, OUTPUT_XML_DUMMY_NARRATIVE),
            Arguments.of(INPUT_JSON_CONCLUSION, OUTPUT_XML_CONCLUSION),
            Arguments.of(INPUT_JSON_CODED_DIAGNOSIS, OUTPUT_XML_CODED_DIAGNOSIS),
            Arguments.of(INPUT_JSON_MULTIPLE_CODED_DIAGNOSIS, OUTPUT_XML_MULTIPLE_CODED_DIAGNOSIS)
        );
    }

    private Answer<String> mockIdForResourceAndId() {
        return invocation -> {
            ResourceType resourceType = invocation.getArgument(0);
            String originalId = invocation.getArgument(1).toString();
            return String.format("II-for-%s-%s", resourceType.name(), originalId);
        };
    }

    private Answer<String> mockIdForReference() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("II-for-%s", reference.getReference());
        };
    }

    private Answer<String> mockSpecimenMapping() {
        return invocation -> {
            Specimen specimen = invocation.getArgument(0);
            return String.format("<!-- Mapped Specimen with id: %s -->", specimen.getId());
        };
    }

}
