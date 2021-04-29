package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class SpecimenMapperTest {

    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/specimen/";
    private static final String DIAGNOSTIC_REPORT_DATE = "2020-10-12";
    private static final String INPUT_OBSERVATION_RELATED_TO_SPECIMEN = "input-observation-1.json";
    private static final String INPUT_OBSERVATION_NOT_RELATED_TO_SPECIMEN = "input-observation-2.json";

    private SpecimenMapper specimenMapper;

    @Mock
    private MessageContext messageContext;
    @Mock
    private IdMapper idMapper;
    @Mock
    private ObservationMapper observationMapper;
    private List<Observation> observations;

    @BeforeEach
    public void setUp() throws IOException {
        lenient().when(messageContext.getIdMapper()).thenReturn(idMapper);
        lenient().when(idMapper.getOrNew(any(ResourceType.class), anyString())).thenAnswer(mockId());
        lenient().when(idMapper.get(any(Reference.class))).thenAnswer(mockReference());

        observations = Arrays.asList(
            parseObservation(INPUT_OBSERVATION_RELATED_TO_SPECIMEN),
            parseObservation(INPUT_OBSERVATION_NOT_RELATED_TO_SPECIMEN)
        );

        when(observationMapper.mapObservationToCompoundStatement(any(), any())).thenAnswer(mockObservationMapping());

        specimenMapper = new SpecimenMapper(messageContext, observationMapper);
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void When_MappingSpecimen_Expect_XmlMapped(String inputPath, String expectedPath) throws IOException {
        var input = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + inputPath);
        var expected = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + expectedPath);
        var specimen = new FhirParseService().parseResource(input, Specimen.class);

        String outputMessage = specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, DIAGNOSTIC_REPORT_DATE);

        assertThat(outputMessage).isEqualTo(expected);
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
            Arguments.of("input-specimen-without-pertinent-information.json", "expected-specimen-without-pertinent-information.xml"),
            Arguments.of("input-specimen-without-fasting-status.json", "expected-specimen-without-fasting-status.xml"),
            Arguments.of("input-specimen-without-fasting-duration.json", "expected-specimen-without-fasting-duration.xml"),
            Arguments.of("input-specimen-without-quantity.json", "expected-specimen-without-quantity.xml"),
            Arguments.of("input-specimen-without-body-site.json", "expected-specimen-without-body-site.xml"),
            Arguments.of("input-specimen-without-notes.json", "expected-specimen-without-notes.xml")
        );
    }

    private Answer<String> mockId() {
        return invocation -> {
            ResourceType resourceType = invocation.getArgument(0);
            String originalId = invocation.getArgument(1);
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
        String fileContent = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + path);

        return new FhirParseService().parseResource(fileContent, Observation.class);
    }
}
