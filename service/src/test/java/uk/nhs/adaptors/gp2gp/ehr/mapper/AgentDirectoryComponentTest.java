package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterEach;
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
public class AgentDirectoryComponentTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String NHS_NUMBER = "9465701459";
    private static final String AGENT_DIRECTORY_FOLDER = "/ehr/mapper/agent-directory/";
    private static final String BUNDLE_NO_ROLES = AGENT_DIRECTORY_FOLDER + "fhir-bundle-no-roles.json";
    private static final String DIRECTORY_NO_ROLES = AGENT_DIRECTORY_FOLDER + "agent-directory-no-roles-component.xml";
    private static final String BUNDLE_WITH_ROLES = AGENT_DIRECTORY_FOLDER + "fhir-bundle-with-roles.json";
    private static final String DIRECTORY_WITH_ROLES = AGENT_DIRECTORY_FOLDER + "agent-directory-with-roles-component.xml";
    private static final String BUNDLE_WITH_RESOURCES_PRACTITIONER_AND_ROLE = AGENT_DIRECTORY_FOLDER
        + "fhir-bundle-no-org-in-resource.json";
    private static final String DIRECTORY_WITH_RESOURCES_PRACTITIONER_AND_ROLE = AGENT_DIRECTORY_FOLDER
        + "agent-directory-mapped-by-practitioner-role.xml";
    private static final String BUNDLE_WITH_RESOURCES_PRACTITIONER_NO_ROLE = AGENT_DIRECTORY_FOLDER
        + "fhir-bundle-no-org-in-resource-or-role.json";
    private static final String DIRECTORY_WITH_RESOURCES_PRACTITIONER_NO_ROLE = AGENT_DIRECTORY_FOLDER
        + "agent-directory-mapped-by-practitioner.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private AgentDirectoryMapper agentDirectoryMapper;
    private FhirParseService fhirParseService;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        OrganizationToAgentMapper organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
        PractitionerAgentPersonMapper practitionerAgentPersonMapper =
            new PractitionerAgentPersonMapper(messageContext, organizationToAgentMapper);
        agentDirectoryMapper = new AgentDirectoryMapper(practitionerAgentPersonMapper, organizationToAgentMapper);
        fhirParseService = new FhirParseService();
    }

    @ParameterizedTest
    @MethodSource("testParams")
    public void When_MappingEhrFolder_Expect_CorrectOutputFromMapper(String input, String output) throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        var expectedOutput = ResourceTestFileUtils.getFileContent(output);
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);
        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> testParams() {
        return Stream.of(
            Arguments.of(BUNDLE_NO_ROLES, DIRECTORY_NO_ROLES),
            Arguments.of(BUNDLE_WITH_ROLES, DIRECTORY_WITH_ROLES),
            Arguments.of(BUNDLE_WITH_RESOURCES_PRACTITIONER_AND_ROLE, DIRECTORY_WITH_RESOURCES_PRACTITIONER_AND_ROLE),
            Arguments.of(BUNDLE_WITH_RESOURCES_PRACTITIONER_NO_ROLE, DIRECTORY_WITH_RESOURCES_PRACTITIONER_NO_ROLE)
        );
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }
}
