package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IIdType;

public class ResourceExtractor {

    public static Optional<Resource> extractResourceByReference(Bundle allBundle, IIdType reference) {
        return allBundle.getEntry()
            .stream()
            .filter(Bundle.BundleEntryComponent::hasResource)
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType().name().equals(reference.getResourceType()))
            .filter(resource -> resource.getIdElement().getIdPart().equals(reference.getIdPart()))
            .findFirst();
    }
}
