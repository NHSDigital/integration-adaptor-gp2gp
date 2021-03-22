package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AgentDirectoryMapperTest {

    private static final String AGENT_DIRECTORY_FOLDER = "/ehr/mapper/agent-directory/";
    private static final String NHS_NUMBER = "9465701459";
    private static final String INVALID_NHS_NUMBER = "9465701451";
    private static final String ORGANIZATION = "<ORGANIZATION/>";
    private static final String AGENT_PERSON = "<AGENT_PERSON/>";

    @Mock
    private OrganizationToAgentMapper organizationToAgentMapper;
    @Mock
    private PractitionerAgentPersonMapper practitionerAgentPersonMapper;
    private AgentDirectoryMapper agentDirectoryMapper;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() {
        fhirParseService = new FhirParseService();
        agentDirectoryMapper = new AgentDirectoryMapper(practitionerAgentPersonMapper, organizationToAgentMapper);
    }

    @Test
    public void When_MappingAgentDirectory_Expect_CorrectAmountOfCallsToChildMappers() throws IOException {
        setupMock(organizationToAgentMapper, practitionerAgentPersonMapper);
        var jsonInput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "fhir-bundle-no-roles.json");
        var expectedOutput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "agent-directory-no-roles.xml");
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);
        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void When_MappingAgentDirectory_Expect_NoDuplicates() throws IOException {
        setupMock(organizationToAgentMapper, practitionerAgentPersonMapper);
        var jsonInput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "fhir-bundle-duplicates.json");
        var expectedOutput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "agent-directory-no-roles.xml");
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        var mapperOutput = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, NHS_NUMBER);
        assertThat(mapperOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void When_IncorrectNhsNumber_Expect_ErrorToBeThrown() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(AGENT_DIRECTORY_FOLDER + "fhir-bundle-duplicates.json");
        Bundle bundle = fhirParseService.parseResource(jsonInput, Bundle.class);
        assertThrows(EhrMapperException.class, () -> agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, INVALID_NHS_NUMBER));
    }

    private static void setupMock(OrganizationToAgentMapper organizationToAgentMapper,
        PractitionerAgentPersonMapper practitionerAgentPersonMapper) {
        when(organizationToAgentMapper.mapOrganizationToAgent(any(Organization.class))).thenReturn(ORGANIZATION);
        when(practitionerAgentPersonMapper.mapPractitionerToAgentPerson(any(Practitioner.class),
            any(Optional.class), any(Optional.class))).thenReturn(AGENT_PERSON);
    }
}
