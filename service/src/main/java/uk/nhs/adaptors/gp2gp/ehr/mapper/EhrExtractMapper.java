package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.util.List;
import java.util.stream.Collectors;

import static uk.nhs.adaptors.gp2gp.ehr.utils.EncounterExtractor.extractEncounterReferencesFromEncounterList;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final EncounterMapper encounterMapper;
    private final AgentDirectoryMapper agentDirectoryMapper;

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    public EhrExtractTemplateParameters mapBundleToEhrFhirExtractParams(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
                                                                        Bundle bundle) {
        final String ehrExtractId = randomIdGeneratorService.createNewId();
        final String ehrFolderId = randomIdGeneratorService.createNewId();
        final String patientId = getGpcStructuredTaskDefinition.getNhsNumber();
        final String requestId = getGpcStructuredTaskDefinition.getRequestId();
        final String toOdsCode = getGpcStructuredTaskDefinition.getToOdsCode();
        final String fromOdsCode = getGpcStructuredTaskDefinition.getFromOdsCode();
        final String availabilityTime = DateFormatUtil.toHl7Format(timestampService.now());
        // must be before mapping components
        final String agentDirectory = agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle,
            getGpcStructuredTaskDefinition.getNhsNumber());
        final List<Encounter> encounters = extractEncounterReferencesFromEncounterList(bundle.getEntry());
        // requires agent directory already mapped
        final List<String> components = mapEncounterToEhrComponents(encounters);

        return EhrExtractTemplateParameters.builder()
            .ehrExtractId(ehrExtractId)
            .ehrFolderId(ehrFolderId)
            .patientId(patientId)
            .requestId(requestId)
            .toOdsCode(toOdsCode)
            .fromOdsCode(fromOdsCode)
            .availabilityTime(availabilityTime)
            .agentDirectory(agentDirectory)
            .components(components)
            .build();
    }

    private List<String> mapEncounterToEhrComponents(List<Encounter> encounters) {
        return encounters.stream()
            .map(encounterMapper::mapEncounterToEhrComposition)
            .collect(Collectors.toList());
    }
}
