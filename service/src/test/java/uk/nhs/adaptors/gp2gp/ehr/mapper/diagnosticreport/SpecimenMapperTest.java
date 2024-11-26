package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility.NOPAT_HL7_CONFIDENTIALITY_CODE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.utils.ConfidentialityCodeUtility;
import uk.nhs.adaptors.gp2gp.utils.FileParsingUtility;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectory;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
class SpecimenMapperTest {
    private static final String BASE_DIRECTORY = "/ehr/mapper/diagnosticreport/";
    private static final String SPECIMEN_TEST_FILES_DIRECTORY = BASE_DIRECTORY + "/specimen/";
    private static final String OBSERVATION_TEST_FILES_DIRECTORY = BASE_DIRECTORY + "/observation/";
    private static final String DIAGNOSTIC_REPORT_DATE = "2020-10-12T13:33:44Z";
    private static final String FHIR_INPUT_BUNDLE = BASE_DIRECTORY + "fhir_bundle.json";
    private static final String INPUT_OBSERVATION_RELATED_TO_SPECIMEN = "input-observation-related-to-specimen.json";
    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String ID_FROM_ID_MAPPER = "some-id";
    private static final DiagnosticReport DIAGNOSTIC_REPORT = new DiagnosticReport().setIssuedElement(
        new InstantType(DIAGNOSTIC_REPORT_DATE)
    );

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
    @Mock
    private ConfidentialityService confidentialityService;
    @InjectMocks
    private SpecimenMapper specimenMapper;

    @BeforeEach
    void setUp() {
        var inputBundleString = ResourceTestFileUtils.getFileContent(FHIR_INPUT_BUNDLE);
        var inputBundle = new FhirParseService().parseResource(inputBundleString, Bundle.class);
        lenient().when(messageContext.getIdMapper()).thenReturn(idMapper);
        lenient().when(messageContext.getAgentDirectory()).thenReturn(agentDirectory);
        lenient().when(messageContext.getInputBundleHolder()).thenReturn(new InputBundle(inputBundle));
        lenient().when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenAnswer(mockId());
        lenient().when(agentDirectory.getAgentId(any(Reference.class))).thenAnswer(mockReference());

        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        observations = List.of(
            getObservationResourceFromJson(INPUT_OBSERVATION_RELATED_TO_SPECIMEN)
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void When_MappingSpecimen_Expect_XmlOutput(String inputPath, String expectedPath) {
        final Specimen specimen = getSpecimenResourceFromJson(inputPath);
        final String expectedXml = ResourceTestFileUtils.getFileContent(SPECIMEN_TEST_FILES_DIRECTORY + expectedPath);

        when(observationMapper.mapObservationToCompoundStatement(any(Observation.class)))
            .thenAnswer(mockObservationMapping());

        final String actualXml = specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, DIAGNOSTIC_REPORT);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    public void When_MappingDefaultSpecimenWithDefaultObservation_Expect_DefaultXmlOutput() {
        final Specimen specimen = getDefaultSpecimen();
        final Observation observation = getDefaultObservation();
        final String expectedXml = ResourceTestFileUtils.getFileContent(
            SPECIMEN_TEST_FILES_DIRECTORY + "expected_output_default_specimen_and_default_observation.xml");

        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class)))
            .thenReturn(ID_FROM_ID_MAPPER);

        when(observationMapper.mapObservationToCompoundStatement(observation))
            .thenAnswer(mockObservationMapping());

        final String actualXml = specimenMapper.mapSpecimenToCompoundStatement(
            specimen, Collections.singletonList(observation), DIAGNOSTIC_REPORT
        );

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    void When_MappingDefaultSpecimenWithNoMappableObservations_Expect_EmptySpecimenXmlOutput() {
        final Specimen specimen = getDefaultSpecimen();
        final String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            SPECIMEN_TEST_FILES_DIRECTORY + "expected_output_default_empty_specimen.xml"
        );

        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class)))
            .thenReturn(ID_FROM_ID_MAPPER);

        final String actualXml = specimenMapper.mapSpecimenToCompoundStatement(
            specimen, Collections.emptyList(), DIAGNOSTIC_REPORT
        );

        assertThat(actualXml).isEqualTo(expectedXmlOutput);
    }

    @Test
    void When_MappingSpecimen_With_NopatMetaSecurity_Expect_ConfidentialityCodeWithinCompoundStatement() {
        final Specimen specimen = getDefaultSpecimen();

        ConfidentialityCodeUtility.appendNopatSecurityToMetaForResource(specimen);

        when(confidentialityService.generateConfidentialityCode(specimen))
            .thenReturn(Optional.of(NOPAT_HL7_CONFIDENTIALITY_CODE));
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class)))
            .thenReturn(ID_FROM_ID_MAPPER);

        final String result = specimenMapper.mapSpecimenToCompoundStatement(specimen,
            Collections.emptyList(), DIAGNOSTIC_REPORT);

        assertAll(
            () -> assertThat(result).contains(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(ConfidentialityCodeUtility.getSecurityCodeFromResource(specimen)).isEqualTo("NOPAT")
        );
    }

    @Test
    void When_MappingSpecimen_With_NoscrubMetaSecurity_Expect_ConfidentialityCodeWithinCompoundStatement() {
        final Specimen specimen = getDefaultSpecimen();

        ConfidentialityCodeUtility.appendNoscrubSecurityToMetaForResource(specimen);

        when(confidentialityService.generateConfidentialityCode(specimen))
            .thenReturn(Optional.empty());
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class)))
            .thenReturn(ID_FROM_ID_MAPPER);

        final String result = specimenMapper.mapSpecimenToCompoundStatement(specimen,
            Collections.emptyList(), DIAGNOSTIC_REPORT);

        assertAll(
            () -> assertThat(result).doesNotContain(NOPAT_HL7_CONFIDENTIALITY_CODE),
            () -> assertThat(ConfidentialityCodeUtility.getSecurityCodeFromResource(specimen)).isEqualTo("NOSCRUB")
        );
    }

    private Specimen getDefaultSpecimen() {
        return getSpecimenResourceFromJson("input_default_specimen.json");
    }

    private Observation getDefaultObservation() {
        return getObservationResourceFromJson("input_default_observation.json");
    }

    private Observation getObservationResourceFromJson(String filename) {
        final String filePath = OBSERVATION_TEST_FILES_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Observation.class);
    }

    private Specimen getSpecimenResourceFromJson(String filename) {
        final String filePath = SPECIMEN_TEST_FILES_DIRECTORY + filename;
        return FileParsingUtility.parseResourceFromJsonFile(filePath, Specimen.class);
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
}
