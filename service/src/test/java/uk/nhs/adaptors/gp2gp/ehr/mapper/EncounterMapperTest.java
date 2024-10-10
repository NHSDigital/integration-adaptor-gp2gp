package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
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
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EncounterMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/encounter/";
    private static final String TEST_ID = "test-id";
    private static final String CONSULTATION_REFERENCE = "F550CC56-EF65-4934-A7B1-3DC2E02243C3";
    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final Date CONSULTATION_DATE = Date.from(Instant.parse("2010-01-13T15:13:32Z"));
    private static final String TEST_LOCATION_NAME = "Test Location";
    private static final String TEST_LOCATION_ID = "EB3994A6-5A87-4B53-A414-913137072F57";

    public static final Bundle.BundleEntryComponent BUNDLE_ENTRY_WITH_CONSULTATION = new Bundle.BundleEntryComponent()
        .setResource(new ListResource()
            .setEncounter(new Reference()
                .setReference(CONSULTATION_REFERENCE))
            .setCode(new CodeableConcept()
                .setCoding(List.of(new Coding()
                    .setCode(CONSULTATION_LIST_CODE))))
            .setDate(CONSULTATION_DATE));

    public static final Bundle.BundleEntryComponent BUNDLE_ENTRY_WITH_LOCATION = new Bundle.BundleEntryComponent()
        .setResource(new Location().setName(TEST_LOCATION_NAME).setId(TEST_LOCATION_ID));

    private static final String SAMPLE_EHR_COMPOSITION_COMPONENT = TEST_FILES_DIRECTORY
        + "sample-ehr-composition-component.xml";

    private static final String INPUT_JSON_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-1.json";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-1.xml";
    private static final String INPUT_JSON_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-2.json";
    private static final String OUTPUT_XML_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-2.xml";
    private static final String INPUT_JSON_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-3.json";
    private static final String OUTPUT_XML_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-3.xml";
    private static final String INPUT_JSON_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "example-encounter-resource-4.json";
    private static final String OUTPUT_XML_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "expected-output-encounter-4.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-5.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-5.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-6.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-6.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-7.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-7.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-8.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-8.xml";
    private static final String INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-9.json";
    private static final String OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-9.xml";
    private static final String INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-10.json";
    private static final String OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-10.xml";
    private static final String INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-11.json";
    private static final String OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-11.xml";
    private static final String INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-12.json";
    private static final String OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT  = TEST_FILES_DIRECTORY
        + "expected-output-encounter-12.xml";
    private static final String INPUT_JSON_WITH_NO_TYPE = TEST_FILES_DIRECTORY
        + "example-encounter-resource-13.json";
    private static final String INPUT_JSON_WITHOUT_RECORDER_PARTICIPANT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-14.json";
    private static final String INPUT_JSON_WITHOUT_PERFORMER_PARTICIPANT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-15.json";
    private static final String INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE = TEST_FILES_DIRECTORY
        + "example-encounter-resource-16.json";
    private static final String INPUT_JSON_WITH_NO_LOCATION_REFERENCE = TEST_FILES_DIRECTORY
        + "example-encounter-resource-17.json";
    private static final String OUTPUT_XML_WITH_NO_LOCATION_REFERENCE  = TEST_FILES_DIRECTORY
        + "expected-output-encounter-14.xml";
    private static final String OUTPUT_XML_WITH_RECORDER_AS_PARTICIPANT2  = TEST_FILES_DIRECTORY
        + "expected-output-encounter-13.xml";
    private static final String INPUT_JSON_ENCOUNTER_NO_ASSOCIATED_CONSULTATION = TEST_FILES_DIRECTORY
        + "example-encounter-resource-18.json";
    private static final String OUTPUT_XML_WITH_NO_EHR_COMPOSITION_COMPONENTS = TEST_FILES_DIRECTORY
        + "expected-output-encounter-14.xml";
    public static final String OUTPUT_XML_WITH_NUL_AUTHOR_PARTICIPANT2 = TEST_FILES_DIRECTORY + "expected-output-encounter-15.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;
    @Mock
    private Bundle bundle;

    private EncounterMapper encounterMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        lenient().when(randomIdGeneratorService.createNewOrUseExistingUUID(anyString())).thenReturn(TEST_ID);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        lenient().when(bundle.getEntry()).thenReturn(List.of(
            BUNDLE_ENTRY_WITH_CONSULTATION,
            BUNDLE_ENTRY_WITH_LOCATION
        ));
        encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedEncounterJson_Expect_EhrCompositionXmlOutput(String input, String output) throws IOException {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);
        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_TIME, OUTPUT_XML_WITH_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_START_EFFECTIVE_TIME, OUTPUT_XML_WITH_START_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_EFFECTIVE_TIME, OUTPUT_XML_WITH_NO_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_PERIOD_FIELD, OUTPUT_XML_WITH_NO_PERIOD_FIELD),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_TEXT, OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT, OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT),
            Arguments.of(INPUT_JSON_WITHOUT_PERFORMER_PARTICIPANT, OUTPUT_XML_WITH_RECORDER_AS_PARTICIPANT2),
            Arguments.of(INPUT_JSON_WITHOUT_RECORDER_PARTICIPANT, OUTPUT_XML_WITH_NUL_AUTHOR_PARTICIPANT2),
            Arguments.of(INPUT_JSON_WITH_NO_LOCATION_REFERENCE, OUTPUT_XML_WITH_NO_LOCATION_REFERENCE)
        );
    }

    @Test
    public void When_MappingEncounterWithNoType_Expect_Exception() throws IOException {
        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_TYPE);

        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        assertThatThrownBy(() -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Could not map Encounter type");
    }

    @Test
    public void When_MappingEncounterWithInvalidParticipantReferenceResourceType_Expect_Exception() throws IOException {
        var sampleComponent = ResourceTestFileUtils.getFileContent(SAMPLE_EHR_COMPOSITION_COMPONENT);

        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_PERFORMER_INVALID_REFERENCE_RESOURCE_TYPE);

        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        when(encounterComponentsMapper.mapComponents(parsedEncounter)).thenReturn(sampleComponent);

        assertThatThrownBy(() -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage("Not supported agent reference: Patient/6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73");
    }

    @Test
    public void When_MappingEmptyConsultation_Expect_NoEhrCompositionGenerated() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_ENCOUNTER_NO_ASSOCIATED_CONSULTATION);
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualTo(StringUtils.EMPTY);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }
}
