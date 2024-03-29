package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class SpecimenMapperTest {

    private static final String DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";
    private static final String DIAGNOSTIC_REPORT_DATE = "2020-10-12T13:33:44Z";
    private static final String FHIR_INPUT_BUNDLE = DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "fhir_bundle.json";

    private static final String INPUT_OBSERVATION_RELATED_TO_SPECIMEN = "input-observation-related-to-specimen.json";
    private static final String INPUT_OBSERVATION_NOT_RELATED_TO_SPECIMEN = "input-observation-not-related-to-specimen.json";

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";

    private SpecimenMapper specimenMapper;
    private List<Observation> observations;

    @Mock
    private MessageContext messageContext;

    @Mock
    private IdMapper idMapper;

    @Mock
    private AgentDirectory agentDirectory;

    @Mock
    private ObservationMapper observationMapper;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @BeforeEach
    public void setUp() throws IOException {
        var inputBundleString = ResourceTestFileUtils.getFileContent(FHIR_INPUT_BUNDLE);
        var inputBundle = new FhirParseService().parseResource(inputBundleString, Bundle.class);
        lenient().when(messageContext.getIdMapper()).thenReturn(idMapper);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(new InputBundle(inputBundle));
        lenient().when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenAnswer(mockId());
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockReference());

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        observations = List.of(
            parseObservation(INPUT_OBSERVATION_RELATED_TO_SPECIMEN),
            parseObservation(INPUT_OBSERVATION_NOT_RELATED_TO_SPECIMEN)
        );

        specimenMapper = new SpecimenMapper(messageContext, observationMapper, randomIdGeneratorService);
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void When_MappingSpecimen_Expect_XmlOutput(String inputPath, String expectedPath) throws IOException {
        var input = ResourceTestFileUtils.getFileContent(DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + inputPath);
        var specimen = new FhirParseService().parseResource(input, Specimen.class);

        var diagnosticReport = new DiagnosticReport().setIssuedElement(new InstantType(DIAGNOSTIC_REPORT_DATE));

        var expected = ResourceTestFileUtils.getFileContent(DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + expectedPath);

        when(observationMapper.mapObservationToCompoundStatement(any())).thenAnswer(mockObservationMapping());

        String outputMessage = specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, diagnosticReport);

        assertThat(outputMessage).isEqualTo(expected);
    }

    @Test
    public void When_MappingDefaultSpecimenWithDefaultObservation_Expect_DefaultXmlOutput() throws IOException {
        String defaultSpecimenJson = ResourceTestFileUtils.getFileContent(
            DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "input_default_specimen.json"
        );
        Specimen specimen = new FhirParseService().parseResource(defaultSpecimenJson, Specimen.class);

        String defaultObservationJson = ResourceTestFileUtils.getFileContent(
            DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "observation/" + "input_default_observation.json"
        );
        Observation observation = new FhirParseService().parseResource(defaultObservationJson, Observation.class);

        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "expected_output_default_specimen_and_default_observation.xml"
        );
        var diagnosticReport = new DiagnosticReport().setIssuedElement(new InstantType(DIAGNOSTIC_REPORT_DATE));

        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");
        when(observationMapper.mapObservationToCompoundStatement(observation)).thenAnswer(mockObservationMapping());

        String compoundStatementXml = specimenMapper.mapSpecimenToCompoundStatement(
            specimen, Collections.singletonList(observation), diagnosticReport
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    @Test
    public void When_MappingDefaultSpecimenWithObservations_Expect_DefaultSpecimenAndObservationsXmlOutput() throws IOException {
        String defaultSpecimenJson = ResourceTestFileUtils.getFileContent(
            DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "input_default_specimen.json"
        );
        Specimen specimen = new FhirParseService().parseResource(defaultSpecimenJson, Specimen.class);

        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "expected_output_default_specimen_with_observations.xml"
        );
        var diagnosticReport = new DiagnosticReport().setIssuedElement(new InstantType(DIAGNOSTIC_REPORT_DATE));

        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");
        when(observationMapper.mapObservationToCompoundStatement(any())).thenAnswer(mockObservationMapping());

        String compoundStatementXml = specimenMapper.mapSpecimenToCompoundStatement(
            specimen, observations, diagnosticReport
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    @Test
    public void When_MappingDefaultSpecimenWithNoMappableObservations_Expect_EmptySpecimenXmlOutput() throws IOException {
        String defaultSpecimenJson = ResourceTestFileUtils.getFileContent(
                DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "input_default_specimen.json"
        );
        Specimen specimen = new FhirParseService().parseResource(defaultSpecimenJson, Specimen.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
                DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "specimen/" + "expected_output_default_empty_specimen.xml"
        );
        var diagnosticReport = new DiagnosticReport().setIssuedElement(new InstantType(DIAGNOSTIC_REPORT_DATE));

        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");

        String compoundStatementXml = specimenMapper.mapSpecimenToCompoundStatement(
                specimen, Collections.emptyList(), diagnosticReport
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of("input-specimen.json", "expected-specimen.xml"),
            Arguments.of("input-specimen-with-collection-period.json", "expected-specimen-with-collection-period.xml"),
            Arguments.of("input-specimen-without-accession-identifier.json", "expected-specimen-without-accession-identifier.xml"),
            Arguments.of("input-specimen-without-collection-time.json", "expected-specimen-without-collection-time.xml"),
            Arguments.of("input-specimen-without-type.json", "expected-specimen-without-type.xml"),
            Arguments.of("input-specimen-without-type-text.json", "expected-specimen-without-type-text.xml"),
            Arguments.of("input-specimen-without-agent-person.json", "expected-specimen-without-agent-person.xml"),
            Arguments.of("input-specimen-without-collection-details.json", "expected-specimen-without-collection-details.xml"),
            Arguments.of("input-specimen-without-fasting-status.json", "expected-specimen-without-fasting-status.xml"),
            Arguments.of("input-specimen-without-fasting-duration.json", "expected-specimen-without-fasting-duration.xml"),
            Arguments.of("input-specimen-without-quantity.json", "expected-specimen-without-quantity.xml"),
            Arguments.of("input-specimen-without-body-site.json", "expected-specimen-without-body-site.xml"),
            Arguments.of("input-specimen-without-notes.json", "expected-specimen-without-notes.xml"),
            Arguments.of("input-specimen-without-effective-time.json", "expected-specimen-without-effective-time.xml"),
            Arguments.of("input-specimen-with-empty-duration-value.json", "expected-specimen-with-empty-duration-value.xml"),
            Arguments.of("input-specimen-with-empty-quantity-value.json", "expected-specimen-with-empty-quantity-value.xml")
        );
    }

    private Answer<String> mockId() {
        return invocation -> {
            ResourceType resourceType = invocation.getArgument(0);
            String originalId = invocation.getArgument(1).toString();
            return String.format("ID-for-%s-%s", resourceType.name(), originalId);
        };
    }

    private Answer<String> mockReference() {
        return invocation -> {
            Reference reference = invocation.getArgument(0);
            return String.format("REFERENCE-to-%s", reference.getReference());
        };
    }

    private Answer<String> mockObservationMapping() {
        return invocation -> {
            Observation observation = invocation.getArgument(0);
            return String.format("<!-- Mapped Observation with id: %s -->", observation.getId());
        };
    }

    private Observation parseObservation(String path) throws IOException {
        String fileContent = ResourceTestFileUtils.getFileContent(DIAGNOSTIC_REPORT_TEST_FILE_DIRECTORY + "observation/" + path);

        return new FhirParseService().parseResource(fileContent, Observation.class);
    }
}
