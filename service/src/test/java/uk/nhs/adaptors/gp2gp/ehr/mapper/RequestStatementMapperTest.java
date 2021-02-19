package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ReferralRequest;
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
public class RequestStatementMapperTest extends MapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/referral/";
    private static final String INPUT_JSON_BUNDLE =  TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-1.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-2.json";
    private static final String INPUT_JSON_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY + "example-referral-request-resource-3.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-4.json";
    private static final String INPUT_JSON_WITH_REASON_CODES = TEST_FILE_DIRECTORY + "example-referral-request-resource-5.json";
    private static final String INPUT_JSON_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY + "example-referral-request-resource-6.json";
    private static final String INPUT_JSON_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-7.json";
    private static final String INPUT_JSON_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-8.json";
    private static final String INPUT_JSON_WITH_PATIENT_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-9.json";
    private static final String INPUT_JSON_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-10.json";
    private static final String INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-resource-11.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-resource-12.json";
    private static final String INPUT_JSON_WITH_NOTES = TEST_FILE_DIRECTORY + "example-referral-request-resource-13.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT = TEST_FILE_DIRECTORY + "example-referral-request-resource-14.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR = TEST_FILE_DIRECTORY + "example-referral-request-resource-15.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-19.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER = TEST_FILE_DIRECTORY + "example-referral-request-resource-16.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT = TEST_FILE_DIRECTORY + "example-referral-request-resource-17.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR = TEST_FILE_DIRECTORY + "example-referral-request-resource-18.json";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-1.xml";
    private static final String OUTPUT_XML_USES_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-2.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY + "expected-output-request-statement-3.xml";
    private static final String OUTPUT_XML_DOES_NOT_USE_DEFAULT_CODE = TEST_FILE_DIRECTORY + "expected-output-request-statement-4.xml";
    private static final String OUTPUT_XML_WITH_REASON_CODES = TEST_FILE_DIRECTORY + "expected-output-request-statement-5.xml";
    private static final String OUTPUT_XML_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY + "expected-output-request-statement-6.xml";
    private static final String OUTPUT_XML_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY + "expected-output-request-statement-7.xml";
    private static final String OUTPUT_XML_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY + "expected-output-request-statement-8.xml";
    private static final String OUTPUT_XML_WITH_PATIENT_REQUESTER  = TEST_FILE_DIRECTORY + "expected-output-request-statement-9.xml";
    private static final String OUTPUT_XML_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY + "expected-output-request-statement-10.xml";
    private static final String OUTPUT_XML_WITH_RECIPIENTS = TEST_FILE_DIRECTORY + "expected-output-request-statement-11.xml";
    private static final String OUTPUT_XML_WITH_RECIPIENTS_AND_PRACTITIONER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-12.xml";
    private static final String OUTPUT_XML_WITH_NOTES = TEST_FILE_DIRECTORY + "expected-output-request-statement-13.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private Bundle bundle;
    private CharSequence expectedOutputMessage;
    private RequestStatementMapper requestStatementMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);

        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        requestStatementMapper = new RequestStatementMapper(messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, false);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_PRACTITIONER_REQUESTER, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_ONE_REASON_CODE, OUTPUT_XML_DOES_NOT_USE_DEFAULT_CODE),
            Arguments.of(INPUT_JSON_WITH_REASON_CODES, OUTPUT_XML_WITH_REASON_CODES),
            Arguments.of(INPUT_JSON_WITH_SERVICES_REQUESTED, OUTPUT_XML_WITH_SERVICES_REQUESTED),
            Arguments.of(INPUT_JSON_WITH_DEVICE_REQUESTER, OUTPUT_XML_WITH_DEVICE_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_ORG_REQUESTER, OUTPUT_XML_WITH_ORG_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_PATIENT_REQUESTER, OUTPUT_XML_WITH_PATIENT_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_RELATION_REQUESTER, OUTPUT_XML_WITH_RELATION_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_RECIPIENTS),
            Arguments.of(INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_RECIPIENTS_AND_PRACTITIONER),
            Arguments.of(INPUT_JSON_WITH_NOTES, OUTPUT_XML_WITH_NOTES),
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_FIELDS, OUTPUT_XML_USES_OPTIONAL_FIELDS)
            );
    }

    @Test
    public void When_MappingReferralRequestJsonWithNestedTrue_Expect_RequestStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, true);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsWithUnexpectedReferences")
    public void When_MappingReferralRequestJsonWithUnexpectedReferences_Expect_Exception(String inputJson) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        assertThrows(EhrMapperException.class, ()
            -> requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, false));
    }

    private static Stream<Arguments> resourceFileParamsWithUnexpectedReferences() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT),
            Arguments.of(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR),
            Arguments.of(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER),
            Arguments.of(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT),
            Arguments.of(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR)
            );
    }
}
