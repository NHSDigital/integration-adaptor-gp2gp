package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.Optional;
import java.util.stream.Stream;

public class ResourceExtractor {

    public static Optional<Resource> extractResourceByReference(Bundle allBundle, IIdType reference) {
        if (ObjectUtils.allNotNull(allBundle, reference)) {
            return allBundle.getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> isReferencedResource(reference, resource))
                .findFirst();
        }

        return Optional.empty();
    }

    public static <T extends Resource> Stream<T> extractResourcesByType(Bundle bundle, Class<T> resourceClass) {
        return bundle.getEntry().stream()
            .filter(Bundle.BundleEntryComponent::hasResource)
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast);
    }

    private static boolean isReferencedResource(IIdType reference, Resource resource) {
        return hasResourceType(resource, reference.getResourceType()) && hasId(reference, resource);
    }

    public static Optional<ListResource> extractListByEncounterReference(Bundle allBundle, IIdType reference, String code) {
        if (ObjectUtils.allNotNull(allBundle, reference, code)) {
            return allBundle.getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> hasResourceType(resource, ResourceType.List.name()))
                .map(ListResource.class::cast)
                .filter(resource -> hasEncounterReference(reference, resource))
                .filter(resource -> hasCode(code, resource))
                .findFirst();
        }

        return Optional.empty();
    }

    private static boolean hasResourceType(Resource resource, String resourceType) {
        return StringUtils.equals(resource.getResourceType().name(), resourceType);
    }

    private static boolean hasId(IIdType reference, Resource resource) {

        return resource.hasIdElement()
               && (StringUtils.equals(resource.getIdElement().getIdPart(), reference.getIdPart())
                   || hasIdMatchInListContainedResources(reference, resource));
    }

    private static boolean hasIdMatchInListContainedResources(IIdType reference, Resource resource) {
        final var listID = resource.getIdElement().getIdPart();
        return StringUtils.equals(ResourceType.List.name(), resource.getResourceType().name())
               &&  ((ListResource)resource)
                   .getContained()
                   .stream()
                   .peek(resource1 -> {
                       System.out.println("ResourceID ID Part: " + listID);
                       System.out.println("Just contained resourceID: " + resource1.getId());
                       System.out.println("Reference ID: " + reference.getIdPart());
                   })
                   .anyMatch(containedResource -> StringUtils.equals(reference.getIdPart(), listID + containedResource.getId()));

    }

    private static boolean hasEncounterReference(IIdType reference, ListResource resource) {
        return resource.hasEncounter()
               && resource.getEncounter().hasReferenceElement()
               && StringUtils.equals(resource.getEncounter().getReferenceElement().getIdPart(), reference.getIdPart());
    }

    private static boolean hasCode(String code, ListResource resource) {
        return resource.hasCode() && resource.getCode().getCoding().stream().anyMatch(coding -> StringUtils.equals(code, coding.getCode()));
    }
}