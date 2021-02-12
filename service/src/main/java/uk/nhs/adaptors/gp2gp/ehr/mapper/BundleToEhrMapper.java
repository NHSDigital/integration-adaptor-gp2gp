package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterListTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class BundleToEhrMapper {
    private static final Mustache EHR_BUNDLE_TO_EHR_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_bundle_to_ehr_template.mustache");
    private static final String CONSULTATION_LIST_CODE = "1149501000000101";

    private final EncounterMapper encounterMapper;

    public String mapBundleToEhr(Bundle bundle) {
        var encounters = extractEncountersByReferences(bundle.getEntry(), extractEncounterReferences(bundle.getEntry()));
        var encounterListTemplateParameters = EncounterListTemplateParameters.builder()
            .components(mapEncounterToEhrComponents(encounters));

        return TemplateUtils.fillTemplate(EHR_BUNDLE_TO_EHR_TEMPLATE,
            encounterListTemplateParameters.build());
    }

    private List<String> extractEncounterReferences(List<Bundle.BundleEntryComponent> entries) {
        return entries.stream()
            .filter(entry -> entry.getResource().getResourceType().equals(ResourceType.List))
            .map(entry -> (ListResource) entry.getResource())
            .filter(this::isConsultationList)
            .findFirst()
            .map(listResource -> extractEncounterReferencesFromListResource(listResource.getEntry()))
            .orElse(Collections.emptyList());
    }

    private boolean isConsultationList(ListResource listResource) {
        return listResource.getCode()
            .getCoding()
            .get(0)
            .getCode()
            .equals(CONSULTATION_LIST_CODE);
    }

    private List<String> extractEncounterReferencesFromListResource(List<ListResource.ListEntryComponent> entries) {
        return entries.stream()
            .map(ListResource.ListEntryComponent::getItem)
            .map(Reference::getReference)
            .collect(Collectors.toList());
    }

    private List<Encounter> extractEncountersByReferences(List<Bundle.BundleEntryComponent> entries, List<String> encounterReferences) {
        return entries.stream()
            .filter(entry -> entry.getResource().getResourceType().equals(ResourceType.Encounter))
            .map(entry -> (Encounter) entry.getResource())
            .filter(encounter -> encounterReferences.contains(encounter.getId()))
            .collect(Collectors.toList());
    }

    private List<String> mapEncounterToEhrComponents(List<Encounter> encounters) {
        return encounters.stream()
            .map(encounterMapper::mapEncounterToEhrComposition)
            .collect(Collectors.toList());
    }
}
