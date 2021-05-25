package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

@Slf4j
@AllArgsConstructor
public class AgentDirectory {
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final Map<AgentKey, String> agentDirectory = new HashMap<>();
    private final Bundle inputBundle;

    public Set<Map.Entry<AgentKey, String>> getEntries() {
        return agentDirectory.entrySet();
    }

    public String getAgentId(Reference reference) {
        if (reference.hasReferenceElement()) {
            String resourceType = reference.getReferenceElement().getResourceType();
            if (ResourceType.Practitioner.name().equals(resourceType)) {
                return putIfAbsent(buildAgentKeyForPractitioner(reference));
            } else if (ResourceType.Organization.name().equals(resourceType)) {
                return putIfAbsent(AgentKey.builder().organizationReference(reference.getReference()).build());
            }
        }

        throw new EhrMapperException("Not supported agent reference: " + reference.getReference());
    }

    public String getAgentRef(Reference practitionerReference, Reference organizationReference) {
        if (ResourceType.Practitioner.name().equals(practitionerReference.getReferenceElement().getResourceType())
            && ResourceType.Organization.name().equals(organizationReference.getReferenceElement().getResourceType())) {
            return putIfAbsent(
                new AgentKey(practitionerReference.getReference(), organizationReference.getReference())
            );
        }

        throw new EhrMapperException("Not supported agent reference to practitioner: " + practitionerReference.getReference()
            + " and organization: " + organizationReference.getReference());
    }

    private AgentKey buildAgentKeyForPractitioner(Reference practitionerReference) {
        return inputBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.PractitionerRole.equals(resource.getResourceType()))
            .map(resource -> (PractitionerRole) resource)
            .filter(resource -> resource.getPractitioner().hasReference())
            .filter(resource -> StringUtils.equals(resource.getPractitioner().getReference(), practitionerReference.getReference()))
            .filter(resource -> resource.getOrganization().hasReference())
            .map(resource -> AgentKey.builder()
                .practitionerReference(resource.getPractitioner().getReference())
                .organizationReference(resource.getOrganization().getReference())
                .build())
            .findFirst()
            .orElseGet(() -> AgentKey.builder().practitionerReference(practitionerReference.getReference()).build());
    }

    private String putIfAbsent(AgentKey agentKey) {
        String newId = randomIdGeneratorService.createNewId();
        String currentId = agentDirectory.putIfAbsent(agentKey, newId);

        return currentId == null ? newId : currentId;
    }

    @Builder
    @Getter
    public static class AgentKey {

        private String practitionerReference;
        private String organizationReference;

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(practitionerReference)
                .append(organizationReference)
                .toHashCode();
        }

        @Override
        public boolean equals(Object agentKey) {
            return agentKey instanceof AgentKey
                && StringUtils.equals(((AgentKey) agentKey).practitionerReference, practitionerReference)
                && StringUtils.equals(((AgentKey) agentKey).organizationReference, organizationReference);
        }
    }
}
