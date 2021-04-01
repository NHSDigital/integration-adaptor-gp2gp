package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractListByEncounterReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractResourceByReference;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IIdType;

public class InputBundle {
    private final Bundle bundle;

    public InputBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Optional<Resource> getResource(IIdType reference) {
        return extractResourceByReference(this.bundle, reference);
    }

    public Optional<ListResource> getListReferencedToEncounter(IIdType reference, String code) {
        return extractListByEncounterReference(this.bundle, reference, code);
    }

    public Optional<Resource> getRelatedCondition(String referenceId) {
        System.out.println("yolo1: " + referenceId);
        for (var entry: bundle.getEntry()) {
            String path = entry.getResource().getResourceType().getPath();
            if (path.equals("condition")) {
                System.out.println(entry.getResource().getId());
                entry.getResource().
                System.out.println("-----------");
                //entry.getResource().children().
                for (var c: entry.getResource().children()) {
                    if (c.getName().equals("extension")) {
                        System.out.println(c.getValues().get(0).get);
                    }
                    //System.out.println(c.getName().equals("extension"));
                }
                System.out.println("-----------");
                System.out.println("condition?" + entry.getResource().children().get(1).getName());
                for (var extension: entry.getExtension()) {
                    var url = extension.getUrl();
                    System.out.println("url: " + url);
                }
            }
            System.out.println("yolo 2: " + path);
        }

        return Optional.empty();
    }
}
