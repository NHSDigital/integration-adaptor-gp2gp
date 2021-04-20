package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public final class EncounterExtractor {
    private static final String ENCOUNTER_LIST_CODE = "1149501000000101";

    public static List<Encounter> extractEncounterReferencesFromEncounterList(Bundle bundle) {
        var encounterReferences = extractEncounterReferencesFromConsultationList(bundle);

        List<Encounter> encountersFromBundle = ResourceExtractor.extractResourcesByType(bundle, Encounter.class)
            .collect(Collectors.toList());

        return prepareSortedReferencedEncounters(encounterReferences, encountersFromBundle);
    }

    private static List<String> extractEncounterReferencesFromConsultationList(Bundle bundle) {
        return ResourceExtractor.extractResourcesByType(bundle, ListResource.class)
            .filter(EncounterExtractor::isConsultationList)
            .findFirst()
            .map(listResource -> extractEncounterReferencesFromListResource(listResource.getEntry()))
            .orElse(Collections.emptyList());
    }

    private static boolean isConsultationList(ListResource listResource) {
        return listResource.hasCode()
            && listResource.getCode().hasCoding()
            && listResource.getCode().getCoding()
            .stream()
            .anyMatch(coding -> coding.hasCode() && coding.getCode().equals(ENCOUNTER_LIST_CODE));
    }

    private static List<String> extractEncounterReferencesFromListResource(List<ListResource.ListEntryComponent> entries) {
        return entries.stream()
            .map(ListResource.ListEntryComponent::getItem)
            .map(Reference::getReference)
            .collect(Collectors.toList());
    }

    private static List<Encounter> prepareSortedReferencedEncounters(
            List<String> encounterReferences, List<Encounter> encountersFromBundle) {

        List<Encounter> sortedReferencedEncounters = new ArrayList<>();

        encounterReferences.forEach(encounterReference -> encountersFromBundle
            .forEach(encounter -> fetchMatchingEncounter(encounterReference, encounter)
                .ifPresent(sortedReferencedEncounters::add)));

        return sortedReferencedEncounters;
    }

    private static Optional<Encounter> fetchMatchingEncounter(String encounterReference, Encounter encounter) {
        if (encounterReference.equals(encounter.getId())) {
            return Optional.of(encounter);
        }

        return Optional.empty();
    }
}
