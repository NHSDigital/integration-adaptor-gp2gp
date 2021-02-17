package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class RequestStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/referral/";
    private static final String INPUT_JSON_BUNDLE =  TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-1.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-2.json";
    private static final String INPUT_JSON_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY + "example-referral-request-resource-3.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-4.json";
    private static final String INPUT_JSON_WITH_REASON_CODES = TEST_FILE_DIRECTORY + "example-referral-request-resource-5.json";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-1.xml";
    private static final String OUTPUT_XML_USES_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-2.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY + "expected-output-request-statement-3.xml";
    private static final String OUTPUT_XML_DOES_NOT_USE_DEFAULT_CODE = TEST_FILE_DIRECTORY + "expected-output-request-statement-4.xml";
    private static final String OUTPUT_XML_WITH_REASON_CODES = TEST_FILE_DIRECTORY + "expected-output-request-statement-5.xml";

    private static final String INPUT_JSON_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY + "example-referral-request-resource-6.json";
    private static final String OUTPUT_XML_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY + "expected-output-request-statement-6.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private Bundle bundle;
    private CharSequence expectedOutputMessage;
    private RequestStatementMapper requestStatementMapper;
    private MessageContext messageContext;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        requestStatementMapper = new RequestStatementMapper(messageContext);
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, bundle,false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PRACTITIONER_REQUESTER, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_ONE_REASON_CODE, OUTPUT_XML_DOES_NOT_USE_DEFAULT_CODE),
            Arguments.of(INPUT_JSON_WITH_REASON_CODES, OUTPUT_XML_WITH_REASON_CODES),
            Arguments.of(INPUT_JSON_WITH_SERVICES_REQUESTED, OUTPUT_XML_WITH_SERVICES_REQUESTED),
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_FIELDS, OUTPUT_XML_USES_OPTIONAL_FIELDS)
            );
    }

    @Test
    public void When_MappingReferralRequestJsonWithNestedTrue_Expect_RequestStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, bundle,true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
