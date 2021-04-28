package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DiagnosticReportMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/diagnostic_report/";
    private static final String INPUT_JSON_BUNDLE = "fhir-bundle.json";
    private static final String INPUT_JSON_REQUIRED_DATA = "diagnostic-report-with-required-data.json";
    private static final String INPUT_JSON_EMPTY_SPECIMENS = "diagnostic-report-with-empty-specimens.json";
    private static final String INPUT_JSON_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.json";
    private static final String INPUT_JSON_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.json";
    private static final String OUTPUT_XML_REQUIRED_DATA = "diagnostic-report-with-required-data.xml";
    private static final String OUTPUT_XML_ONE_SPECIMEN = "diagnostic-report-with-one-specimen.xml";
    private static final String OUTPUT_XML_MULTI_SPECIMENS = "diagnostic-report-with-multi-specimens.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private DiagnosticReportMapper mapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        final String bundleInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + INPUT_JSON_BUNDLE);
        final Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);

        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        final ObservationMapper observationMapper = new ObservationMapper(messageContext,
            new StructuredObservationValueMapper(), codeableConceptCdMapper, new ParticipantMapper());
        final SpecimenMapper specimenMapper = new SpecimenMapper(messageContext,
            observationMapper);
        mapper = new DiagnosticReportMapper(messageContext, specimenMapper);
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
        return Stream.of(Arguments.of(INPUT_JSON_REQUIRED_DATA, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_EMPTY_SPECIMENS, OUTPUT_XML_REQUIRED_DATA),
            Arguments.of(INPUT_JSON_ONE_SPECIMEN, OUTPUT_XML_ONE_SPECIMEN),
            Arguments.of(INPUT_JSON_MULTI_SPECIMENS, OUTPUT_XML_MULTI_SPECIMENS));
    }
}
