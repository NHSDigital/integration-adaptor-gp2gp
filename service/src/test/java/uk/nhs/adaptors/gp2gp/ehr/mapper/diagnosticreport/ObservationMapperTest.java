package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObservationMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/diagnosticreport/";

    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_JSON = TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_1.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_2_JSON = TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_2.json";
    private static final String OBSERVATION_ASSOCIATED_WITH_SPECIMEN_3_JSON = TEST_FILE_DIRECTORY
        + "observation_associated_with_specimen_3.json";

    private static final String OBSERVATION_COMPOUND_STATEMENT_1_XML = TEST_FILE_DIRECTORY
        + "observation_compound_statement_1.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_2_XML = TEST_FILE_DIRECTORY
        + "observation_compound_statement_2.xml";
    private static final String OBSERVATION_COMPOUND_STATEMENT_3_XML = TEST_FILE_DIRECTORY
        + "observation_compound_statement_3.xml";

    private List<Observation> observations;

    @Mock
    private IdMapper idMapper;

    @Mock
    private MessageContext messageContext;

    private ObservationMapper observationMapper;

    @BeforeEach
    public void setUp() throws IOException {
        String bundleJsonInput = ResourceTestFileUtils.getFileContent(TEST_FILE_DIRECTORY + "fhir_bundle.json");
        Bundle bundle = new FhirParseService().parseResource(bundleJsonInput, Bundle.class);
        observations = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().equals(ResourceType.Observation))
            .map(Observation.class::cast)
            .collect(Collectors.toList());

        when(messageContext.getIdMapper()).thenReturn(idMapper);

        observationMapper = new ObservationMapper(
            messageContext,
            new StructuredObservationValueMapper(),
            new CodeableConceptCdMapper(),
            new ParticipantMapper()
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_CompoundStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");
        when(idMapper.get(any(Reference.class))).thenReturn("some-reference");

        String jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(outputXml);

        String compoundStatementXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen,
            observations
        );

        assertThat(compoundStatementXml).isEqualToIgnoringWhitespace(expectedXmlOutput);
    }

    @Test
    public void When_MappingDefaultObservationJson_Expect_DefaultObservationStatementXmlOutput() throws IOException {
        when(idMapper.getOrNew(any(ResourceType.class), any(IdType.class))).thenReturn("some-id");

        String jsonInput = ResourceTestFileUtils.getFileContent(
            TEST_FILE_DIRECTORY + "input_default_observation.json"
        );
        Observation observationAssociatedWithSpecimen = new FhirParseService().parseResource(jsonInput, Observation.class);
        String expectedXmlOutput = ResourceTestFileUtils.getFileContent(
            TEST_FILE_DIRECTORY + "expected_output_default_observation.xml"
        );

        String compoundStatementXml = observationMapper.mapObservationToCompoundStatement(
            observationAssociatedWithSpecimen,
            Collections.emptyList()
        );

        assertThat(compoundStatementXml).isEqualTo(expectedXmlOutput);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_1_JSON, OBSERVATION_COMPOUND_STATEMENT_1_XML),
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_2_JSON, OBSERVATION_COMPOUND_STATEMENT_2_XML),
            Arguments.of(OBSERVATION_ASSOCIATED_WITH_SPECIMEN_3_JSON, OBSERVATION_COMPOUND_STATEMENT_3_XML)
        );
    }
}
