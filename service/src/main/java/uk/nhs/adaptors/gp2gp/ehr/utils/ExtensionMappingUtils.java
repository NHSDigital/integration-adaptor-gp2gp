package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;

public class ExtensionMappingUtils {

    public static Optional<Extension> filterExtensionByUrl(DomainResource resource, String url) {
        return resource.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(url))
            .findFirst();
    }
}
