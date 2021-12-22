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

import ca.uhn.fhir.context.FhirContext;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractListByEncounterReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractResourceByReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.IgnoredResourcesUtils.isIgnoredResourceType;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputBundle {
    private static final String ACTUAL_PROBLEM_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1";
    private static final  List<ResourceType> MAPPABLE_RESOURCES = List.of(
        ResourceType.AllergyIntolerance,
        ResourceType.Appointment,
        ResourceType.Condition,
        ResourceType.Device,
        ResourceType.DiagnosticReport,
        ResourceType.DocumentReference,
        ResourceType.Encounter,
        ResourceType.HealthcareService,
        ResourceType.Immunization,
        ResourceType.List,
        ResourceType.Location,
        ResourceType.Medication,
        ResourceType.MedicationRequest,
        ResourceType.Observation,
        ResourceType.Organization,
        ResourceType.Patient,
        ResourceType.Practitioner,
        ResourceType.PractitionerRole,
        ResourceType.ProcedureRequest,
        ResourceType.ReferralRequest,
        ResourceType.RelatedPerson,
        ResourceType.Specimen
        );
    private boolean called = false;

    private final Bundle bundle;

    public InputBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public List<Resource> getResourcesOfType(Class<?> classType) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.valueOf(classType.getSimpleName()).equals(resource.getResourceType()))
            .collect(Collectors.toList());
    }

    public Optional<Resource> getResource(IIdType reference) {
        if (reference.getResourceType() == null) {
            return Optional.empty();
        }

        var resourceType = ResourceType.fromCode(reference.getResourceType());

        if (MAPPABLE_RESOURCES.contains(resourceType)) {
            Optional<Resource> resource = extractResourceByReference(this.bundle, reference);
            if (resource.isPresent()) {
                return resource;
            }
            throw new EhrMapperException("Resource not found: " + reference);
        } else if (isIgnoredResourceType(resourceType)) {
            LOGGER.info(String.format("Resource of type: %s has been ignored", resourceType.name()));
            return Optional.empty();
        } else {
            throw new EhrMapperException("Reference not supported resource type: " + reference);
        }
    }

    public Resource getRequiredResource(IIdType reference) {
        if (!called) {
            FhirContext ctx = FhirContext.forDstu3();

            String serialized = ctx.newJsonParser().encodeResourceToString(bundle);
            //System.out.println(serialized);
            LOGGER.info("\n\n" + serialized + "\n\n");
            called = true;
        }

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
