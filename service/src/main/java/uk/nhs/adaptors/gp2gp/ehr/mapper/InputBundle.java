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
}
