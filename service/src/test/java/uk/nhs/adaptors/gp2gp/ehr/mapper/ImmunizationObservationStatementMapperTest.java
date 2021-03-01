package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Immunization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ImmunizationObservationStatementMapperTest extends MapperTest {
    private static final String TEST_ID = "test-id";

    private static final String IMMUNIZATION_FILE_LOCATIONS = "/ehr/mapper/immunization/";
    private static final String INPUT_JSON_WITH_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-all-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-pertinent-information.json";
    private static final String INPUT_JSON_WITHOUT_DATE_RECORDED_EXTENSION = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-date-recorded.json";
    private static final String INPUT_JSON_WITHOUT_CODEABLE_CONCEPT_TEXT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-codeable-concepts-text.json";
    private static final String INPUT_JSON_WITHOUT_DATE = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-no-date.json";
    private static final String INPUT_JSON_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-reason-not-given-coding.json";
    private static final String INPUT_JSON_REASON_NOT_GIVEN_TEXT = IMMUNIZATION_FILE_LOCATIONS
        + "immunization-reason-not-given-text.json";
    private static final String INPUT_JSON_BUNDLE = IMMUNIZATION_FILE_LOCATIONS + "fhir-bundle.json";

    private static final String OUTPUT_XML_WITH_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-all-information.xml";
    private static final String OUTPUT_XML_WITHOUT_PERTINENT_INFORMATION = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-no-information.xml";
    private static final String OUTPUT_XML_WITHOUT_CONTEXT = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-with-context.xml";
    private static final String OUTPUT_XML_WITHOUT_DATE = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-no-date.xml";
    private static final String OUTPUT_XML_WITH_REASON_NOT_GIVEN = IMMUNIZATION_FILE_LOCATIONS
        + "expected-output-observation-statement-reason-not-given.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MessageContext messageContext;
    private ImmunizationObservationStatementMapper observationStatementMapper;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        fhirParseService = new FhirParseService();
        messageContext = new MessageContext(randomIdGeneratorService);
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = fhirParseService.parseResource(bundleInput, Bundle.class);
        messageContext.initialize(bundle);
        observationStatementMapper = new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingImmunizationJson_Expect_ObservationStatementXmlOutput(String inputJson, String outputXml,
        boolean isNested) throws IOException {
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);

        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);
        String outputMessage = observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, isNested);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PERTINENT_INFORMATION, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITHOUT_CODEABLE_CONCEPT_TEXT, OUTPUT_XML_WITH_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_WITHOUT_DATE, OUTPUT_XML_WITHOUT_DATE, false),
            Arguments.of(INPUT_JSON_WITHOUT_PERTINENT_INFORMATION, OUTPUT_XML_WITHOUT_PERTINENT_INFORMATION, false),
            Arguments.of(INPUT_JSON_REASON_NOT_GIVEN, OUTPUT_XML_WITH_REASON_NOT_GIVEN, false),
            Arguments.of(INPUT_JSON_REASON_NOT_GIVEN_TEXT, OUTPUT_XML_WITH_REASON_NOT_GIVEN, false),
            Arguments.of(INPUT_JSON_WITH_PERTINENT_INFORMATION, OUTPUT_XML_WITHOUT_CONTEXT, true)
        );
    }

    @Test
    public void When_MappingParsedImmunizationJsonWithoutDateRecordedExtension_Expect_Error() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITHOUT_DATE_RECORDED_EXTENSION);
        Immunization parsedImmunization = fhirParseService.parseResource(jsonInput, Immunization.class);

        assertThrows(EhrMapperException.class, ()
            -> observationStatementMapper.mapImmunizationToObservationStatement(parsedImmunization, false));
    }
}
