package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ReferralRequest;
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

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.CodeableConceptMapperMockUtil;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class RequestStatementMapperTest {
    private static final String COMMON_ID_1 = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";
    private static final String PATIENT_2_ID = "4C903809-CADA-442E-8FA5-655E5EFEB1F5";
    private static final String RELATED_PERSON_ID = "f002";
    private static final String ORGANIZATION_ID = "1F90B10F-CF14-4D6F-8C0D-585059DA4EC5";

    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/referral/";

    // INPUT FILES
    private static final String INPUT_JSON_BUNDLE =  TEST_FILE_DIRECTORY + "fhir-bundle.json";
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "example-referral-request-no-optional-fields.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "example-referral-request-with-optional-fields.json";
    private static final String INPUT_JSON_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY
        + "example-referral-request-with-one-reason-code.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-practitioner-requester.json";
    private static final String INPUT_JSON_WITH_REASON_CODES = TEST_FILE_DIRECTORY
        + "example-referral-request-with-reason-codes.json";
    private static final String INPUT_JSON_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY
        + "example-referral-request-with-services-requested.json";
    private static final String INPUT_JSON_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-device-requester.json";
    private static final String INPUT_JSON_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-org-requester.json";
    private static final String INPUT_JSON_WITH_PATIENT_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-patient-requester.json";
    private static final String INPUT_JSON_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-relation-requester.json";
    private static final String INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-one-practitioner-recipient.json";
    private static final String INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-multiple-practitioner-recipients.json";
    private static final String INPUT_JSON_WITH_NOTES = TEST_FILE_DIRECTORY
        + "example-referral-request-with-notes.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-recipient.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-author.json";
    private static final String INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-with-incorrect-resource-type-requester.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-requester.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-recipient.json";
    private static final String INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR = TEST_FILE_DIRECTORY
        + "example-referral-request-no-resolved-reference-note-author.json";
    private static final String INPUT_JSON_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF = TEST_FILE_DIRECTORY
        + "example-referral-request-no-onbehalfof.json";

    // OUTPUT FILES
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-optional-fields.xml";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS_NESTED = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-optional-fields-nested.xml";
    private static final String OUTPUT_XML_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-optional-fields.xml";
    private static final String OUTPUT_XML_WITH_ONE_REASON_CODE = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-one-reason-code.xml";
    private static final String OUTPUT_XML_WITH_PRACTITIONER_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-practitioner-requester.xml";
    private static final String OUTPUT_XML_WITH_REASON_CODES = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-reason-codes.xml";
    private static final String OUTPUT_XML_WITH_SERVICES_REQUESTED = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-services-requested.xml";
    private static final String OUTPUT_XML_WITH_DEVICE_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-device-requester.xml";
    private static final String OUTPUT_XML_WITH_ORG_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-org-requester.xml";
    private static final String OUTPUT_XML_WITH_PATIENT_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-patient-requester.xml";
    private static final String OUTPUT_XML_WITH_RELATION_REQUESTER = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-relation-requester.xml";
    private static final String OUTPUT_XML_WITH_ONE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-one-practitioner-recipient.xml";
    private static final String OUTPUT_XML_WITH_MULTIPLE_PRACTITIONER_RECIPIENT = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-multiple-practitioner-recipients.xml";
    private static final String OUTPUT_XML_WITH_NOTES = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-with-notes.xml";
    private static final String OUTPUT_XML_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF = TEST_FILE_DIRECTORY
        + "expected-output-request-statement-no-onbehalfof.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private CodeableConceptCdMapper codeableConceptCdMapper;

    private RequestStatementMapper requestStatementMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        Bundle bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);

        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(bundle);
        var idMapper = messageContext.getIdMapper();
        idMapper.getOrNew(ResourceType.Practitioner, COMMON_ID_1);
        idMapper.getOrNew(ResourceType.Organization, COMMON_ID_1);
        idMapper.getOrNew(ResourceType.Device, COMMON_ID_1);
        idMapper.getOrNew(ResourceType.Patient, COMMON_ID_1);
        idMapper.getOrNew(ResourceType.Patient, PATIENT_2_ID);
        idMapper.getOrNew(ResourceType.RelatedPerson, RELATED_PERSON_ID);
        idMapper.getOrNew(ResourceType.Organization, ORGANIZATION_ID);
        requestStatementMapper = new RequestStatementMapper(messageContext, codeableConceptCdMapper, new ParticipantMapper());
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
            arguments(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            arguments(INPUT_JSON_WITH_OPTIONAL_FIELDS, OUTPUT_XML_WITH_OPTIONAL_FIELDS),
            arguments(INPUT_JSON_WITH_PRACTITIONER_REQUESTER, OUTPUT_XML_WITH_PRACTITIONER_REQUESTER),
            arguments(INPUT_JSON_WITH_SERVICES_REQUESTED, OUTPUT_XML_WITH_SERVICES_REQUESTED),
            arguments(INPUT_JSON_WITH_DEVICE_REQUESTER, OUTPUT_XML_WITH_DEVICE_REQUESTER),
            arguments(INPUT_JSON_WITH_ORG_REQUESTER, OUTPUT_XML_WITH_ORG_REQUESTER),
            arguments(INPUT_JSON_WITH_PATIENT_REQUESTER, OUTPUT_XML_WITH_PATIENT_REQUESTER),
            arguments(INPUT_JSON_WITH_RELATION_REQUESTER, OUTPUT_XML_WITH_RELATION_REQUESTER),
            arguments(INPUT_JSON_WITH_ONE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_ONE_PRACTITIONER_RECIPIENT),
            arguments(INPUT_JSON_WITH_MULTIPLE_PRACTITIONER_RECIPIENT, OUTPUT_XML_WITH_MULTIPLE_PRACTITIONER_RECIPIENT),
            arguments(INPUT_JSON_WITH_NOTES, OUTPUT_XML_WITH_NOTES),
            arguments(INPUT_JSON_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF, OUTPUT_XML_WITH_PRACTITIONER_REQUESTER_NO_ONBEHALFOF)
            );
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsReasonCodes")
    public void When_MappingObservationJsonWithReason_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) {
        when(codeableConceptCdMapper.mapCodeableConceptToCd(any(CodeableConcept.class)))
            .thenReturn(CodeableConceptMapperMockUtil.NULL_FLAVOR_CODE);
        assertThatInputMapsToExpectedOutput(inputJson, outputXml);
    }

    private static Stream<Arguments> resourceFileParamsReasonCodes() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_ONE_REASON_CODE, OUTPUT_XML_WITH_ONE_REASON_CODE),
            Arguments.of(INPUT_JSON_WITH_REASON_CODES, OUTPUT_XML_WITH_REASON_CODES)
        );
    }

    @Test
    public void When_MappingReferralRequestJsonWithNestedTrue_Expect_RequestStatementXmlOutput() throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NO_OPTIONAL_FIELDS_NESTED);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, true);

        assertThat(outputMessage).isEqualTo(expectedOutputMessage);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParamsWithUnexpectedReferences")
    public void When_MappingReferralRequestJsonWithUnexpectedReferences_Expect_Exception(String inputJson, String exceptionMessage)
            throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        ReferralRequest parsedReferralRequest = new FhirParseService().parseResource(jsonInput, ReferralRequest.class);

        assertThatThrownBy(() -> requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, false))
            .isExactlyInstanceOf(EhrMapperException.class)
            .hasMessage(exceptionMessage);
    }

    private static Stream<Arguments> resourceFileParamsWithUnexpectedReferences() {
        return Stream.of(
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_REQUESTER, "Requester Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_RECIPIENT, "Recipient Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_INCORRECT_RESOURCE_TYPE_AUTHOR, "Author Reference not of expected Resource Type"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_REQUESTER, "Could not resolve Device Reference"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_RECIPIENT, "Could not resolve Organization Reference"),
            arguments(INPUT_JSON_WITH_NO_RESOLVED_REFERENCE_NOTE_AUTHOR, "Could not resolve RelatedPerson Reference")
            );
    }

    @SneakyThrows
    private void assertThatInputMapsToExpectedOutput(String inputJsonResourcePath, String outputXmlResourcePath) {
        var expected = ResourceTestFileUtils.getFileContent(outputXmlResourcePath);
        var input = ResourceTestFileUtils.getFileContent(inputJsonResourcePath);
        var referralRequest = new FhirParseService().parseResource(input, ReferralRequest.class);

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(referralRequest, false);

        assertThat(outputMessage).isEqualTo(expected);
    }

}
