package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class MedicationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/medication_request/";
    private static final String INPUT_JSON_BUNDLE =  TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_INVALID_INTENT = TEST_FILE_DIRECTORY + "medication-request-with-invalid-intent.json";
    private static final String INPUT_JSON_WITH_INVALID_PRESCRIPTION_TYPE = TEST_FILE_DIRECTORY + "medication-request-with-invalid-prescription-type.json";

    private static final String INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "medication-request-with-plan-no-optional-fields.json";
    private static final String OUTPUT_XML_WITH_PRESCRIBE_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "medication-statement-with-plan-no-optional-fields.xml";

    private static final String INPUT_JSON_WITH_ORDER_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "medication-request-with-order-no-optional-fields.json";
    private static final String OUTPUT_XML_WITH_AUTHROISE_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "medication-statement-with-authorise-no-optional-fields.xml";

    @Mock
    private RandomIdGeneratorService mockRandomIdGeneratorService;

    private Bundle bundle;
    private MessageContext messageContext;
    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MedicationStatementMapper medicationStatementMapper;

    @BeforeEach
    public void setUp() throws IOException {
        when(mockRandomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(mockRandomIdGeneratorService);
        messageContext.initialize(bundle);
        medicationStatementMapper = new MedicationStatementMapper(messageContext, codeableConceptCdMapper, mockRandomIdGeneratorService);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        assertThatInputMapsToExpectedOutput(inputJson, outputXml);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_PLAN_NO_OPTIONAL_FIELDS, OUTPUT_XML_WITH_PRESCRIBE_NO_OPTIONAL_FIELDS)
        );
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var parsedMedicationRequest = new FhirParseService().parseResource(input, MedicationRequest.class);

        String outputMessage = medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest);

        assertThat(outputMessage).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("resourceFileExpectException")
    public void When_MappingMedicationRequestWithInvalidResource_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        MedicationRequest parsedMedicationRequest = new FhirParseService().parseResource(jsonInput, MedicationRequest.class);

        RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();
        medicationStatementMapper = new MedicationStatementMapper(messageContext, codeableConceptCdMapper, randomIdGeneratorService);

        assertThrows(EhrMapperException.class, ()
            -> medicationStatementMapper.mapMedicationRequestToMedicationStatement(parsedMedicationRequest));
    }

    private static Stream<Arguments> resourceFileExpectException() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_INVALID_INTENT),
            Arguments.of(INPUT_JSON_WITH_INVALID_PRESCRIPTION_TYPE)
        );
    }

}
