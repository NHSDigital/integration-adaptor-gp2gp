package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
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
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AllergyStructureMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/allergy/";
    private static final String INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-1.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-2.json";
    private static final String INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-3.json";
    private static final String INPUT_JSON_WITH_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-4.json";
    private static final String INPUT_JSON_WITH_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-5.json";
    private static final String INPUT_JSON_WITH_ONSET_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-6.json";
    private static final String INPUT_JSON_WITH_REASON_END_DATE_ONLY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-7.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-8.json";
    private static final String INPUT_JSON_WITH_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-9.json";
    private static final String INPUT_JSON_WITH_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY
        + "example-allergy-intolerance-resource-10.json";
    private static final String INPUT_JSON_WITH_REACTION = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-11.json";
    private static final String INPUT_JSON_WITH_NO_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-12.json";
    private static final String INPUT_JSON_WITH_UNSUPPORTED_CATEGORY = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-13.json";
    private static final String INPUT_JSON_WITH_NO_ASSERTED_DATE = TEST_FILE_DIRECTORY + "example-allergy-intolerance-resource-14.json";
    private static final String OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-1.xml";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-2.xml";
    private static final String OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER = TEST_FILE_DIRECTORY
        + "expected-output-allergy-structure-3.xml";
    private static final String OUTPUT_XML_USES_DATES = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-4.xml";
    private static final String OUTPUT_XML_USES_ONSET_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-5.xml";
    private static final String OUTPUT_XML_USES_NULL_FLAVOR_DATE = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-6.xml";
    private static final String OUTPUT_XML_USES_ENVIRONMENT_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-7.xml";
    private static final String OUTPUT_XML_USES_MEDICATION_CATEGORY = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-8.xml";
    private static final String OUTPUT_XML_USES_REACTION = TEST_FILE_DIRECTORY + "expected-output-allergy-structure-9.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CharSequence expectedOutputMessage;
    private AllergyStructureMapper allergyStructureMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        allergyStructureMapper = new AllergyStructureMapper(messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingAllergyIntoleranceJson_Expect_AllergyStructureXmlOutput(String inputJson, String outputXml) throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        AllergyIntolerance parsedAllergyIntolerance = new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);

        String outputMessage = allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(parsedAllergyIntolerance);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_TEXT_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PATIENT_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_PATIENT_RECORDER_AND_ASSERTER),
            Arguments.of(INPUT_JSON_WITH_RECORDER_AND_ASSERTER, OUTPUT_XML_USES_NO_OPTIONAL_TEXT_FIELDS),
            Arguments.of(INPUT_JSON_WITH_DATES, OUTPUT_XML_USES_DATES),
            Arguments.of(INPUT_JSON_WITH_ONSET_DATE_ONLY, OUTPUT_XML_USES_ONSET_DATE),
            Arguments.of(INPUT_JSON_WITH_REASON_END_DATE_ONLY, OUTPUT_XML_USES_NULL_FLAVOR_DATE),
            Arguments.of(INPUT_JSON_WITH_NO_DATES, OUTPUT_XML_USES_NULL_FLAVOR_DATE),
            Arguments.of(INPUT_JSON_WITH_ENVIRONMENT_CATEGORY, OUTPUT_XML_USES_ENVIRONMENT_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_MEDICATION_CATEGORY, OUTPUT_XML_USES_MEDICATION_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_REACTION, OUTPUT_XML_USES_REACTION)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceInvalidFileParams")
    public void When_MappingInvalidAllergyIntoleranceJson_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        AllergyIntolerance parsedAllergyIntolerance = new FhirParseService().parseResource(jsonInput, AllergyIntolerance.class);

        assertThrows(EhrMapperException.class, ()
            -> allergyStructureMapper.mapAllergyIntoleranceToAllergyStructure(parsedAllergyIntolerance));
    }

    private static Stream<Arguments> resourceInvalidFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_UNSUPPORTED_CATEGORY),
            Arguments.of(INPUT_JSON_WITH_NO_ASSERTED_DATE)
            );
    }
}
