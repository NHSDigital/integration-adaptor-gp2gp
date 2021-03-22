package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AgentDirectoryComponentTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String AGENT_DIRECTORY_FOLDER = "/ehr/mapper/agent-directory/";
    private static final String NHS_NUMBER = "9465701459";

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

    @Test
    public void When_MappingEhrFolderWithoutPractitionerRoles_Expect_AgentPersonsToBeMapped() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "fhir-bundle-no-roles.json");
        var expectedOutput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "agent-directory-no-roles-component.xml");
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);
        assertThat(mapperOutput).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    public void When_MappingEhrFolderWithPractitionerRoles_Expect_OrganizationsToBeMapped() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "fhir-bundle-with-roles.json");
        var expectedOutput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "agent-directory-with-roles-component.xml");
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);
        assertThat(mapperOutput).isEqualToIgnoringWhitespace(expectedOutput);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }
}
