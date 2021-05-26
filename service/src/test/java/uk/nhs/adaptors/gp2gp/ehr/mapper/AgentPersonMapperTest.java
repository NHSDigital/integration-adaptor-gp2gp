package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.utils.TestArgumentsLoaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AgentPersonMapperTest {

    private static final String TEST_ID = "6D340A1B-BC15-4D4E-93CF-BBCB5B74DF73";
    private static final String PRACTITIONER_FILE_LOCATION = "/ehr/mapper/practitioner/";
    private static final String PRACTITIONER_ONLY_FILE_LOCATION = PRACTITIONER_FILE_LOCATION + "practitioner-only/";
    private static final String PRACTITIONER_ROLE_FILE_LOCATION = PRACTITIONER_FILE_LOCATION + "practitioner-role/";
    private static final String ORGANIZATION = PRACTITIONER_FILE_LOCATION + "organization.json";
    private static final String PRACTITIONER = PRACTITIONER_FILE_LOCATION + "practitioner.json";
    private static final String EXPECTED_AGENT_PERSON_WITH_ORGANIZATION = PRACTITIONER_FILE_LOCATION
        + "expected-agent-person-with-organization-only.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private AgentPersonMapper agentPersonMapper;
    private MessageContext messageContext;
    private FhirParseService fhirParseService;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        agentPersonMapper = new AgentPersonMapper(messageContext);
        fhirParseService = new FhirParseService();
    }

    @ParameterizedTest
    @MethodSource("readPractitionerRoleTests")
    public void When_MappingPractitionerToAgent_Expect_RepresentedAgentXml(String practitionerRoleJson, String outputXml)
        throws IOException {

        var jsonInputPractitionerRole = ResourceTestFileUtils.getFileContent(practitionerRoleJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        AgentDirectory.AgentKey agentKey = setUpDataWithOrganizationAndPractitionerRole(jsonInputPractitionerRole);

        var outputMessage = agentPersonMapper.mapAgentPerson(agentKey, TEST_ID);
        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, practitionerRoleJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("readPractitionerOnlyTests")
    public void When_MappingPractitionerOnlyToAgent_Expect_NoRepresentedOrganization(String inputJson, String outputXml)
        throws IOException {
        var jsonInputPractitioner = ResourceTestFileUtils.getFileContent(inputJson);
        var expectedOutput = ResourceTestFileUtils.getFileContent(outputXml);

        AgentDirectory.AgentKey agentKey = setUpDataWithPractitionerOnly(jsonInputPractitioner);

        var outputMessage = agentPersonMapper
            .mapAgentPerson(agentKey, TEST_ID);

        assertThat(outputMessage)
            .describedAs(TestArgumentsLoaderUtil.FAIL_MESSAGE, inputJson, outputXml)
            .isEqualToIgnoringWhitespace(expectedOutput);
    }

    @Test
    public void When_MappingOrganizationOnlyToAgent_Expect_NoPractitionerData() throws IOException {
        var expectedOutput = ResourceTestFileUtils.getFileContent(EXPECTED_AGENT_PERSON_WITH_ORGANIZATION);
        AgentDirectory.AgentKey agentKey = setUpDataWithOrganization();

        var outputMessage = agentPersonMapper.mapAgentPerson(agentKey, TEST_ID);

        assertThat(outputMessage).isEqualTo(expectedOutput);
    }

    private AgentDirectory.AgentKey setUpDataWithOrganization() throws IOException {
        Organization organization = getOrganizationResource();

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organization);
        messageContext.initialize(bundle);

        return new AgentDirectory.AgentKey.AgentKeyBuilder()
            .organizationReference(ResourceType.Organization.name() + "/" + organization.getIdElement().getIdPart())
            .build();
    }

    private static Stream<Arguments> readPractitionerOnlyTests() {
        return TestArgumentsLoaderUtil.readTestCases(PRACTITIONER_ONLY_FILE_LOCATION);
    }

    private AgentDirectory.AgentKey setUpDataWithOrganizationAndPractitionerRole(String jsonInputPractitionerRole) throws IOException {
        Practitioner practitioner = getPractitionerResource();
        PractitionerRole practitionerRole = fhirParseService.parseResource(jsonInputPractitionerRole, PractitionerRole.class);
        Organization organization = getOrganizationResource();

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(practitioner);
        bundle.addEntry().setResource(practitionerRole);
        bundle.addEntry().setResource(organization);
        messageContext.initialize(bundle);

        return new AgentDirectory.AgentKey.AgentKeyBuilder()
            .practitionerReference(ResourceType.Practitioner.name() + "/" + practitioner.getIdElement().getIdPart())
            .organizationReference(ResourceType.Organization.name() + "/" + organization.getIdElement().getIdPart())
            .build();
    }

    private AgentDirectory.AgentKey setUpDataWithPractitionerOnly(String jsonInputPractitioner) {
        Practitioner practitioner = fhirParseService.parseResource(jsonInputPractitioner, Practitioner.class);

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(practitioner);
        messageContext.initialize(bundle);

        return new AgentDirectory.AgentKey.AgentKeyBuilder()
            .practitionerReference(ResourceType.Practitioner.name() + "/" + practitioner.getIdElement().getIdPart())
            .build();
    }

    private static Stream<Arguments> readPractitionerRoleTests() {
        return TestArgumentsLoaderUtil.readTestCases(PRACTITIONER_ROLE_FILE_LOCATION);
    }

    private static Practitioner getPractitionerResource() throws IOException {
        String jsonPractitioner = ResourceTestFileUtils.getFileContent(PRACTITIONER);
        return new FhirParseService().parseResource(jsonPractitioner, Practitioner.class);
    }

    private static Organization getOrganizationResource() throws IOException {
        String jsonOrganization = ResourceTestFileUtils.getFileContent(ORGANIZATION);
        return new FhirParseService().parseResource(jsonOrganization, Organization.class);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }
}
