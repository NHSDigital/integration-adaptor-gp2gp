package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Organization;
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
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrganizationToAgentMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String ORGANIZATION_FILE_LOCATION = "/ehr/mapper/organization/";
    private static final String ORGANIZATION_INNER_FILE_LOCATION = ORGANIZATION_FILE_LOCATION + "inner_mapping/";
    private static final String ORGANIZATION_FOR_INNER_MAPPER_JSON = ORGANIZATION_INNER_FILE_LOCATION
        + "organization_with_address.json";
    private static final String ORGANIZATION_FOR_INNER_MAPPER_XML = ORGANIZATION_INNER_FILE_LOCATION
        + "organization_with_address.xml";

    private OrganizationToAgentMapper organizationToAgentMapper;
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
    }

    @ParameterizedTest
    @MethodSource("readTestCases")
    public void When_MappingOrganization_Expect_AgentResourceXml(String inputJson, String outputXml) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        Organization organization = new FhirParseService().parseResource(jsonInput, Organization.class);
        var outputMessage = organizationToAgentMapper.mapOrganizationToAgent(organization);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    public void When_MappingOrganizationInner_Expect_AgentResourceXmlInner() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(ORGANIZATION_FOR_INNER_MAPPER_JSON);
        var expectedOutput = ResourceTestFileUtils.getFileContent(ORGANIZATION_FOR_INNER_MAPPER_XML);

        Organization organization = new FhirParseService().parseResource(jsonInput, Organization.class);
        var outputMessage = organizationToAgentMapper.mapOrganizationToAgentInner(organization);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutput);
    }

    private static Stream<Arguments> readTestCases() {
        return TestArgumentsLoaderUtil.readTestCases(ORGANIZATION_FILE_LOCATION);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }
}
