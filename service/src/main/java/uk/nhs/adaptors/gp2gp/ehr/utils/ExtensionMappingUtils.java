package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Extension;

public class ExtensionMappingUtils {

    public static Optional<Extension> filterExtensionByUrl(DomainResource resource, String url) {
        return resource.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(url))
            .findFirst();
    }

    public static List<Extension> filterAllExtensionsByUrl(DomainResource resource, String url) {
        return resource.getExtension()
            .stream()
            .filter(extension -> extension.getUrl().equals(url))
            .collect(Collectors.toList());
    }
}
