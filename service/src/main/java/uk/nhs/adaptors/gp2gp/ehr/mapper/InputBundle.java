package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractListByEncounterReference;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor.extractResourceByReference;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;

public class InputBundle {
    private static final String ACTUAL_PROBLEM_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ActualProblem-1";
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

    public List<Condition> getRelatedConditions(String referenceId) {
        return bundle.getEntry()
            .stream()
            .filter(Bundle.BundleEntryComponent::hasResource)
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.Condition.equals(resource.getResourceType()))
            .map(resource -> (Condition) resource)
            .filter(condition -> {
                Optional<String> reference = filterExtensionByUrl(condition, ACTUAL_PROBLEM_URL)
                    .map(Extension::getValue)
                    .map(Reference.class::cast)
                    .map(Reference::getReference)
                    .stream().findFirst();
                return reference.equals(Optional.of(referenceId));
            })
            .collect(Collectors.toList());
    }
}
