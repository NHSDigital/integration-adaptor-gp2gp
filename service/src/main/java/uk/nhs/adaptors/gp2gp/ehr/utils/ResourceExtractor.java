package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;

public class ResourceExtractor {

    public static Optional<Resource> extractResourceByReference(Bundle allBundle, String reference) {
        return allBundle.getEntry()
            .stream()
            .filter(Bundle.BundleEntryComponent::hasResource)
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getId().equals(reference))
            .findFirst();
    }
}
