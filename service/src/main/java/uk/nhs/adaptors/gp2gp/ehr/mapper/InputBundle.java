package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractListByEncounterReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractResourceByReference;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public class InputBundle {
    private static final String ACTUAL_PROBLEM_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1";
    private final Bundle bundle;

    public InputBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Optional<Resource> getResource(IIdType reference) {
        if (reference == null || reference.getResourceType() == null) {
            return Optional.empty();
        }

        var resourceType = ResourceType.fromCode(reference.getResourceType());

        if (reference.getResourceType().equals(resourceType.toString())) {
            return extractResourceByReference(this.bundle, reference);
        } else {
            throw new EhrMapperException("Resource not found: " + reference);
        }
    }

    public Resource getRequiredResource(IIdType reference) {
        return extractResourceByReference(this.bundle, reference)
            .orElseThrow(() -> new EhrMapperException("Resource not found: " + reference));
    }

    public Optional<ListResource> getListReferencedToEncounter(IIdType reference, String code) {
        return extractListByEncounterReference(this.bundle, reference, code);
    }

    public List<Condition> getRelatedConditions(String referenceId) {
        return ResourceExtractor.extractResourcesByType(bundle, Condition.class)
            .filter(condition -> filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL)
                    .map(Extension::getValue)
                    .map(Reference.class::cast)
                    .map(Reference::getReference)
                    .filter(referenceId::equals)
                    .isPresent()
            )
            .collect(Collectors.toList());
    }

    public Optional<PractitionerRole> getPractitionerRoleFor(String practitionerReference, String organizationReference) {
        return ResourceExtractor.extractResourcesByType(bundle, PractitionerRole.class)
            .filter(PractitionerRole::hasPractitioner)
            .filter(PractitionerRole::hasOrganization)
            .filter(practitionerRole -> isPractitionerRoleOf(practitionerReference, organizationReference, practitionerRole))
            .findFirst();
    }

    private boolean isPractitionerRoleOf(String practitionerReference, String organizationReference, PractitionerRole practitionerRole) {
        return StringUtils.equals(practitionerReference, practitionerRole.getPractitioner().getReference())
            && StringUtils.equals(organizationReference, practitionerRole.getOrganization().getReference());
    }
}
