package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
    private static final String INPUT_JSON_WITH_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-1.json";
    private static final String INPUT_JSON_WITH_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "example-referral-request-resource-2.json";
    private static final String OUTPUT_XML_USES_NO_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-1.xml";
    private static final String OUTPUT_XML_USES_OPTIONAL_FIELDS = TEST_FILE_DIRECTORY + "expected-output-request-statement-2.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CharSequence expectedOutputMessage;
    private RequestStatementMapper requestStatementMapper;
    private MessageContext messageContext;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        requestStatementMapper = new RequestStatementMapper(messageContext);
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

        String outputMessage = requestStatementMapper.mapReferralRequestToRequestStatement(parsedReferralRequest, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_NO_OPTIONAL_FIELDS, OUTPUT_XML_USES_NO_OPTIONAL_FIELDS),
            Arguments.of(INPUT_JSON_WITH_OPTIONAL_FIELDS, OUTPUT_XML_USES_OPTIONAL_FIELDS)
            );
    }
}
