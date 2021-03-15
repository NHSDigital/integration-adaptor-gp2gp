package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import java.io.IOException;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AgentDirectoryMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";

    @Mock
    RandomIdGeneratorService randomIdGeneratorService;

    @Test
    public void testing() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        MessageContext messageContext = new MessageContext(randomIdGeneratorService);
        OrganizationToAgentMapper organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
        PractitionerAgentPersonMapper practitionerAgentPersonMapper = new PractitionerAgentPersonMapper(messageContext, organizationToAgentMapper);
        AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(practitionerAgentPersonMapper, organizationToAgentMapper);

        var jsonInput = ResourceTestFileUtils.getFileContent("/ehr/mapper/agent-directory/fhir-bundle.json");

        FhirParseService fps = new FhirParseService();
        Bundle bundle = fps.parseResource(jsonInput, Bundle.class);

        agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, "9465701459");
    }

}
