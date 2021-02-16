package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;

public class ExtractBundleResourceUtil {
    public static Optional<Resource> extractResourceFromBundle(Bundle bundle, String relativeReference) {
        return bundle.getEntry()
            .stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> buildRelativeReference(resource).equals(relativeReference))
            .findFirst();
    }

    private static String buildRelativeReference(Resource resource) {
        if (resource.hasIdElement()) {
            IdType idType = resource.getIdElement();
            return idType.getResourceType() + "/" + idType.getIdPart();
        }
        return StringUtils.EMPTY;
    }
}
