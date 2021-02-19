package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Organization;
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
public class OragnizationAgentMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String ORGANIZATION_FILE_LOCATION = "/ehr/mapper/organization/";
    private static final String ORGANIZATION_JSON = ORGANIZATION_FILE_LOCATION + "example_organization_";
    private static final String ORGANIZATION_XML = ORGANIZATION_FILE_LOCATION + "expected_output_agent_";
    private static final String INPUT_JSON_WITH_IDENTIFIER = ORGANIZATION_JSON + "1.json";
    private static final String OUTPUT_XML_WITH_IDENTIFIER = ORGANIZATION_XML + "1.xml";
    private static final String INPUT_JSON_WITHOUT_IDENTIFIER = ORGANIZATION_JSON + "2.json";
    private static final String OUTPUT_XML_WITHOUT_IDENTIFIER = ORGANIZATION_XML + "2.xml";
    private static final String INPUT_JSON_WITH_TELECOM_IN_0 = ORGANIZATION_JSON + "3.json";
    private static final String OUTPUT_XML_WITH_TELECOM = ORGANIZATION_XML + "3.xml";
    private static final String INPUT_JSON_WITHOUT_TELECOM = ORGANIZATION_JSON + "4.json";
    private static final String OUTPUT_XML_WITHOUT_TELECOM = ORGANIZATION_XML + "4.xml";
    private static final String INPUT_JSON_WITH_TELECOM_IN_2 = ORGANIZATION_JSON + "5.json";
    private static final String OUTPUT_XML_WITH_TELECOM_2 = ORGANIZATION_XML + "5.xml";
    private static final String INPUT_JSON_WITH_ADDRESS = ORGANIZATION_JSON + "6.json";
    private static final String OUTPUT_XML_WITH_ADDRESS = ORGANIZATION_XML + "6.xml";
    private static final String INPUT_JSON_WITHOUT_ADDRESS = ORGANIZATION_JSON + "7.json";
    private static final String OUTPUT_XML_WITHOUT_ADDRESS = ORGANIZATION_XML + "7.xml";
    private static final String INPUT_JSON_WITH_USE_HOME = ORGANIZATION_JSON + "8.json";
    private static final String OUTPUT_XML_USE_HOME = ORGANIZATION_XML + "8.xml";
    private static final String INPUT_JSON_WITH_USE_TEMP = ORGANIZATION_JSON + "9.json";
    private static final String OUTPUT_XML_USE_TEMP = ORGANIZATION_XML + "9.xml";
    private static final String INPUT_JSON_WITH_USE_EMPTY = ORGANIZATION_JSON + "10.json";
    private static final String OUTPUT_XML_USE_EMPTY = ORGANIZATION_XML + "10.xml";

    private OrganizationToAgentMapper organizationToAgentMapper;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @BeforeEach
    public void setUp() {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        MessageContext messageContext = new MessageContext(randomIdGeneratorService);
        organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void When_MappingOrganization_Expect_AgentResourceXml(String inputJson, String outputXml) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Organization organization = new FhirParseService().parseResource(jsonInput, Organization.class);
        var outputMessage = organizationToAgentMapper.mapOrganizationToAgent(organization);
        System.out.println(outputMessage);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> testArguments() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_IDENTIFIER, OUTPUT_XML_WITH_IDENTIFIER),
            Arguments.of(INPUT_JSON_WITHOUT_IDENTIFIER, OUTPUT_XML_WITHOUT_IDENTIFIER),
            Arguments.of(INPUT_JSON_WITH_TELECOM_IN_0, OUTPUT_XML_WITH_TELECOM),
            Arguments.of(INPUT_JSON_WITHOUT_TELECOM, OUTPUT_XML_WITHOUT_TELECOM),
            Arguments.of(INPUT_JSON_WITH_TELECOM_IN_2, OUTPUT_XML_WITH_TELECOM_2),
            Arguments.of(INPUT_JSON_WITH_ADDRESS, OUTPUT_XML_WITH_ADDRESS),
            Arguments.of(INPUT_JSON_WITHOUT_ADDRESS, OUTPUT_XML_WITHOUT_ADDRESS),
            Arguments.of(INPUT_JSON_WITH_USE_HOME, OUTPUT_XML_USE_HOME),
            Arguments.of(INPUT_JSON_WITH_USE_TEMP, OUTPUT_XML_USE_TEMP),
            Arguments.of(INPUT_JSON_WITH_USE_EMPTY, OUTPUT_XML_USE_EMPTY)
        );
    }

}
