package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildReference;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class AgentDirectoryTest {

    private static final String INPUT_BUNDLE = "/ehr/mapper/agent-directory/input-bundle-with-practitioner-roles.json";
    private static final String GENERATED_ID_1 = "GENERATED_ID_1";
    private static final String GENERATED_ID_2 = "GENERATED_ID_2";
    private static final String PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE = "0000-0000-1111-1111";
    private static final String PRACTITIONER_REFERENCE_NOT_IN_PRACTITIONER_ROLE = "Practitioner/0000-0000-1111-1111";
    private static final String PRACTITIONER_ID_IN_PRACTITIONER_ROLE = "1111-2222-3333-4444";
    private static final String PRACTITIONER_REFERENCE_IN_PRACTITIONER_ROLE = "Practitioner/1111-2222-3333-4444";
    private static final String ORGANIZATION_REFERENCE_IN_PRACTITIONER_ROLE = "Organization/id-of-the-same-organization";
    private static final String ORGANIZATION_REFERENCE = "Organization/9999-8888-7777-6666";
    private static final String ORGANIZATION_ID = "9999-8888-7777-6666";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    private Bundle inputBundle;

    @BeforeEach
    public void setUp() throws IOException {
        lenient().when(randomIdGeneratorService.createNewId()).thenReturn(GENERATED_ID_1, GENERATED_ID_2);

        String jsonInput = ResourceTestFileUtils.getFileContent(INPUT_BUNDLE);
        inputBundle = new FhirParseService(FhirContext.forDstu3()).parseResource(jsonInput, Bundle.class);
    }

    @Test
    public void When_GettingPractitionerReference_Expect_ReferenceMappedToId() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE));

        assertPractitionerNotInPractitionerRoleSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingAlreadyMappedPractitionerReference_Expect_NoReferenceDuplicated() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE));
        assertPractitionerNotInPractitionerRoleSavedInAgentDirectory(agentDirectory);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE));
        assertPractitionerNotInPractitionerRoleSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingPractitionerReferenceThatIsInPractitionerRole_Expect_ReferenceMappedToId() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_IN_PRACTITIONER_ROLE));

        assertPractitionerReferenceInPractitionerRoleSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingAlreadyMappedPractitionerReferenceThatIsInPractitionerRole_Expect_NoReferenceDuplicated() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_IN_PRACTITIONER_ROLE));
        assertPractitionerReferenceInPractitionerRoleSavedInAgentDirectory(agentDirectory);

        agentDirectory.getAgentId(buildReference(ResourceType.Practitioner, PRACTITIONER_ID_IN_PRACTITIONER_ROLE));
        assertPractitionerReferenceInPractitionerRoleSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingOrganizationReference_Expect_ReferenceMappedToId() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Organization, ORGANIZATION_ID));

        assertOrganizationReferenceSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingAlreadyMappedOrganizationReference_Expect_NoReferenceDuplicated() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(buildReference(ResourceType.Organization, ORGANIZATION_ID));
        assertOrganizationReferenceSavedInAgentDirectory(agentDirectory);

        agentDirectory.getAgentId(buildReference(ResourceType.Organization, ORGANIZATION_ID));
        assertOrganizationReferenceSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingReferenceToPractitionerAndOrganization_Expect_ReferencesMappedToId() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentRef(
            buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE),
            buildReference(ResourceType.Organization, ORGANIZATION_ID)
        );
        assertReferenceToPractitionerAndOrganizationSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingAlreadyMappedReferenceToPractitionerAndOrganization_Expect_NoReferenceDuplicated() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentRef(
            buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE),
            buildReference(ResourceType.Organization, ORGANIZATION_ID)
        );
        assertReferenceToPractitionerAndOrganizationSavedInAgentDirectory(agentDirectory);

        agentDirectory.getAgentRef(
            buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE),
            buildReference(ResourceType.Organization, ORGANIZATION_ID)
        );
        assertReferenceToPractitionerAndOrganizationSavedInAgentDirectory(agentDirectory);
    }

    @Test
    public void When_GettingReferenceToPractitionerAndOrganizationAndSingleOrganizationIsAlreadyMapped_Expect_ReferencesMappedToId() {
        AgentDirectory agentDirectory = new AgentDirectory(randomIdGeneratorService, inputBundle);

        agentDirectory.getAgentId(
            buildReference(ResourceType.Organization, ORGANIZATION_ID)
        );
        assertOrganizationReferenceSavedInAgentDirectory(agentDirectory);

        agentDirectory.getAgentRef(
            buildReference(ResourceType.Practitioner, PRACTITIONER_ID_NOT_IN_PRACTITIONER_ROLE),
            buildReference(ResourceType.Organization, ORGANIZATION_ID)
        );

        assertPractitionerWithOrganizationSavedInAgentDirectory(agentDirectory);
    }

    private void assertPractitionerWithOrganizationSavedInAgentDirectory(AgentDirectory agentDirectory) {
        assertThat(agentDirectory.getEntries().size()).isEqualTo(2);
        AgentDirectory.AgentKey expectedAgentKey1 = AgentDirectory.AgentKey.builder()
            .organizationReference(ORGANIZATION_REFERENCE)
            .build();

        assertAgentKeySavedInAgentDirectory(agentDirectory, expectedAgentKey1, GENERATED_ID_1);

        AgentDirectory.AgentKey expectedAgentKey2 = AgentDirectory.AgentKey.builder()
            .practitionerReference(PRACTITIONER_REFERENCE_NOT_IN_PRACTITIONER_ROLE)
            .organizationReference(ORGANIZATION_REFERENCE)
            .build();
        assertAgentKeySavedInAgentDirectory(agentDirectory, expectedAgentKey2, GENERATED_ID_2);
    }

    private void assertAgentKeySavedInAgentDirectory(AgentDirectory agentDirectory, AgentDirectory.AgentKey expectedAgentKey,
        String generatedId) {
        Optional<Map.Entry<AgentDirectory.AgentKey, String>> secondAgentKeyStringEntry = agentDirectory.getEntries().stream()
            .filter(entry -> entry.getKey().equals(expectedAgentKey))
            .findFirst();
        assertThat(secondAgentKeyStringEntry.isPresent()).isTrue();
        assertThat(secondAgentKeyStringEntry.get().getKey()).isEqualTo(expectedAgentKey);
        assertThat(secondAgentKeyStringEntry.get().getValue()).isEqualTo(generatedId);
    }

    private void assertPractitionerNotInPractitionerRoleSavedInAgentDirectory(AgentDirectory agentDirectory) {
        AgentDirectory.AgentKey expectedAgentKey = AgentDirectory.AgentKey.builder()
            .practitionerReference(PRACTITIONER_REFERENCE_NOT_IN_PRACTITIONER_ROLE).build();

        assertSingleElementSavedInAgentDirectory(agentDirectory, expectedAgentKey);
    }

    private void assertPractitionerReferenceInPractitionerRoleSavedInAgentDirectory(AgentDirectory agentDirectory) {
        AgentDirectory.AgentKey expectedAgentKey = AgentDirectory.AgentKey.builder()
            .practitionerReference(PRACTITIONER_REFERENCE_IN_PRACTITIONER_ROLE)
            .organizationReference(ORGANIZATION_REFERENCE_IN_PRACTITIONER_ROLE)
            .build();

        assertSingleElementSavedInAgentDirectory(agentDirectory, expectedAgentKey);
    }

    private void assertOrganizationReferenceSavedInAgentDirectory(AgentDirectory agentDirectory) {
        AgentDirectory.AgentKey expectedAgentKey = AgentDirectory.AgentKey.builder()
            .organizationReference(ORGANIZATION_REFERENCE).build();

        assertSingleElementSavedInAgentDirectory(agentDirectory, expectedAgentKey);
    }

    private void assertReferenceToPractitionerAndOrganizationSavedInAgentDirectory(AgentDirectory agentDirectory) {
        AgentDirectory.AgentKey expectedAgentKey = AgentDirectory.AgentKey.builder()
            .practitionerReference(PRACTITIONER_REFERENCE_NOT_IN_PRACTITIONER_ROLE)
            .organizationReference(ORGANIZATION_REFERENCE)
            .build();

        assertSingleElementSavedInAgentDirectory(agentDirectory, expectedAgentKey);
    }

    private void assertSingleElementSavedInAgentDirectory(AgentDirectory agentDirectory, AgentDirectory.AgentKey expectedAgentKey) {
        assertThat(agentDirectory.getEntries().size()).isEqualTo(1);

        Optional<Map.Entry<AgentDirectory.AgentKey, String>> entry = agentDirectory.getEntries().stream().findFirst();
        assertThat(entry.isPresent()).isTrue();
        assertThat(entry.get().getKey()).isEqualTo(expectedAgentKey);
        assertThat(entry.get().getValue()).isEqualTo(GENERATED_ID_1);
    }
}
