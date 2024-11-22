package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrganizationToAgentMapperTest {

    private static final String TEST_ID = "5E496953-065B-41F2-9577-BE8F2FBD0757";
    private static final String ORGANIZATION_FILE_LOCATION = "/ehr/mapper/organization/";

    private static final String INPUT_ORGANIZATION_JSON = ORGANIZATION_FILE_LOCATION
        + "input_organization_1.json";
    private static final String OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON = ORGANIZATION_FILE_LOCATION
        + "output_organization_1.xml";

    @Test
    public void When_MappingOrganization_Expect_AgentResourceXml() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_ORGANIZATION_JSON);
        var expectedOutput = ResourceTestFileUtils.getFileContent(OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON);

        Organization organization = new FhirParseService(FhirContext.forDstu3()).parseResource(jsonInput, Organization.class);
        var outputMessage = OrganizationToAgentMapper.mapOrganizationToAgent(organization, TEST_ID);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, INPUT_ORGANIZATION_JSON, OUTPUT_ORGANIZATION_AS_AGENT_PERSON_JSON)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }
}
