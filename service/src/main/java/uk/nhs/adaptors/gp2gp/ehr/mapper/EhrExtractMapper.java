package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.EncounterExtractor;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");

    private final FhirParseService fhirParseService;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final EncounterMapper encounterMapper;

    public EhrExtractTemplateParameters mapJsonToEhrFhirExtractParams(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            String json) {
        Bundle bundle = fhirParseService.parseResource(json, Bundle.class);

        return prepareEhrFhirExtractParamsFromFhirBundle(getGpcStructuredTaskDefinition, bundle);
    }

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    private EhrExtractTemplateParameters prepareEhrFhirExtractParamsFromFhirBundle(
            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            Bundle bundle) {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = new EhrExtractTemplateParameters();
        ehrExtractTemplateParameters.setEhrExtractId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setEhrFolderId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setPatientId(getGpcStructuredTaskDefinition.getNhsNumber());
        ehrExtractTemplateParameters.setRequestId(getGpcStructuredTaskDefinition.getRequestId());
        ehrExtractTemplateParameters.setToOdsCode(getGpcStructuredTaskDefinition.getToOdsCode());
        ehrExtractTemplateParameters.setFromOdsCode(getGpcStructuredTaskDefinition.getFromOdsCode());
        ehrExtractTemplateParameters.setAvailabilityTime(DateFormatUtil.formatDate(timestampService.now()));

        extractPatientFromBundle(bundle)
            .orElseThrow(() -> new FhirValidationException("Missing patient resource in Fhir Bundle."));

        var encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(bundle.getEntry());
        ehrExtractTemplateParameters.setComponents(mapEncounterToEhrComponents(encounters));

        return ehrExtractTemplateParameters;
    }

    private Optional<Patient> extractPatientFromBundle(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .filter(entry -> !entry.isEmpty())
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType() == ResourceType.Patient)
            .map(resource -> (Patient) resource)
            .findFirst();
    }

    private List<String> mapEncounterToEhrComponents(List<Encounter> encounters) {
        return encounters.stream()
            .map(encounterMapper::mapEncounterToEhrComposition)
            .collect(Collectors.toList());
    }
}
