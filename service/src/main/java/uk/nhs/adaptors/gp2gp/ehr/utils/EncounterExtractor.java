package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.stereotype.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public final class EncounterExtractor {
    private static final String CONSULTATION_LIST_CODE = "1149501000000101";

    public static List<Encounter> extractConsultationListEncounters(List<Bundle.BundleEntryComponent> entries) {
        var encounterReferences = extractEncounterReferencesFromConsultationList(entries);
        return entries.stream()
            .filter(entry -> entry.getResource().getResourceType().equals(ResourceType.Encounter))
            .map(entry -> (Encounter) entry.getResource())
            .filter(encounter -> encounterReferences.contains(encounter.getId()))
            .collect(Collectors.toList());
    }

    private static List<String> extractEncounterReferencesFromConsultationList(List<Bundle.BundleEntryComponent> entries) {
        return entries.stream()
            .filter(entry -> entry.getResource().getResourceType().equals(ResourceType.List))
            .map(entry -> (ListResource) entry.getResource())
            .filter(EncounterExtractor::isConsultationList)
            .findFirst()
            .map(listResource -> extractEncounterReferencesFromListResource(listResource.getEntry()))
            .orElse(Collections.emptyList());
    }

    private static boolean isConsultationList(ListResource listResource) {
        return listResource.getCode()
            .getCoding()
            .stream()
            .anyMatch(coding -> coding.hasCode() && coding.getCode().equals(CONSULTATION_LIST_CODE));
    }

    private static List<String> extractEncounterReferencesFromListResource(List<ListResource.ListEntryComponent> entries) {
        return entries.stream()
            .map(ListResource.ListEntryComponent::getItem)
            .map(Reference::getReference)
            .collect(Collectors.toList());
    }
}
