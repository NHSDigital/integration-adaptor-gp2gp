package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DiagnosticReportMapperTest {
    private static final String DIAGNOSTIC_REPORT_FILE_LOCATION = "/ehr/mapper/diagnosticreport/";

    private static final String INPUT_JSON_BUNDLE = DIAGNOSTIC_REPORT_FILE_LOCATION + "fhir_bundle.json";
    private static final String INPUT_JSON_DIAGNOSTIC_REPORT = DIAGNOSTIC_REPORT_FILE_LOCATION + "diagnostic_report.json";
//    private static final String OUTPUT_XML = DIAGNOSTIC_REPORT_FILE_LOCATION
//        + "output.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @Mock
    private SpecimenMapper specimenMapper;

    private MessageContext messageContext;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() throws IOException {
        fhirParseService = new FhirParseService();
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @Test
    public void When_MappingDiagnosticReportJson_Expect_CompoundStatementXmlOutput() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        var jsonDiagnosticReportInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_DIAGNOSTIC_REPORT);
//        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);

        DiagnosticReport diagnosticReport = fhirParseService.parseResource(jsonDiagnosticReportInput, DiagnosticReport.class);

        DiagnosticReportMapper diagnosticReportMapper = new DiagnosticReportMapper(messageContext, specimenMapper);

        String outputMessage = diagnosticReportMapper.mapDiagnosticReportToCompoundStatement(diagnosticReport);

//        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }
}
