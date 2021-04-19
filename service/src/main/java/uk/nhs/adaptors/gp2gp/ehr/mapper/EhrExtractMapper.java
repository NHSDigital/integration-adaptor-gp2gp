package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
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

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final EncounterMapper encounterMapper;
    private final AgentDirectoryMapper agentDirectoryMapper;
    private final MessageContext messageContext;

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    public EhrExtractTemplateParameters mapBundleToEhrFhirExtractParams(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
        Bundle bundle) {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = new EhrExtractTemplateParameters();
        ehrExtractTemplateParameters.setEhrExtractId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setEhrFolderId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setPatientId(getGpcStructuredTaskDefinition.getNhsNumber());
        ehrExtractTemplateParameters.setRequestId(getGpcStructuredTaskDefinition.getRequestId());
        ehrExtractTemplateParameters.setToOdsCode(getGpcStructuredTaskDefinition.getToOdsCode());
        ehrExtractTemplateParameters.setFromOdsCode(getGpcStructuredTaskDefinition.getFromOdsCode());
        ehrExtractTemplateParameters.setAvailabilityTime(DateFormatUtil.toHl7Format(timestampService.now()));
        ehrExtractTemplateParameters.setAgentDirectory(agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle,
            getGpcStructuredTaskDefinition.getNhsNumber()));

        var encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(bundle.getEntry());
        ehrExtractTemplateParameters.setComponents(mapEncounterToEhrComponents(encounters));

        EhrFolderEffectiveTime effectiveTime = messageContext.getEffectiveTime();
        Optional<String> effectiveTimeLow = effectiveTime.getEffectiveTimeLow();
        effectiveTimeLow.ifPresent(ehrExtractTemplateParameters::setEffectiveTimeLow);

        Optional<String> effectiveTimeHigh = effectiveTime.getEffectiveTimeHigh();
        effectiveTimeHigh.ifPresent(ehrExtractTemplateParameters::setEffectiveTimeHigh);

        ehrExtractTemplateParameters.setHasEffectiveTime(effectiveTimeLow.isPresent() || effectiveTimeHigh.isPresent());

        return ehrExtractTemplateParameters;
    }

    private List<String> mapEncounterToEhrComponents(List<Encounter> encounters) {
        return encounters.stream()
            .map(encounterMapper::mapEncounterToEhrComposition)
            .collect(Collectors.toList());
    }
}
