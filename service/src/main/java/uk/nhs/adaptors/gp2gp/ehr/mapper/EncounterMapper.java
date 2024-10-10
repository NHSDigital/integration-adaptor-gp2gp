package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class EncounterMapper {
    private static final Mustache ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_encounter_to_ehr_composition_template.mustache");
    private static final String COMPLETE_CODE = "COMPLETE";
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final String OTHER_REPORT_CODE = "24591000000103";
    private static final String OTHER_REPORT_DISPLAY = "Other report";
    private static final Set<String> EHR_COMPOSITION_NAME_VOCABULARY_CODES = getEhrCompositionNameVocabularyCodes();

    private final MessageContext messageContext;
    private final EncounterComponentsMapper encounterComponentsMapper;

    public String mapEncounterToEhrComposition(Encounter encounter) {
        LOGGER.debug("Generating ehrComposition for Encounter {}", encounter.getId());
        String components = encounterComponentsMapper.mapComponents(encounter);

        if (StringUtils.isBlank(components)) {
            LOGGER.info("Skipping Encounter with ID '{}'. The mapping output contains blank XML statement content",
                encounter.getId());
            return StringUtils.EMPTY;
        }

        final IdMapper idMapper = messageContext.getIdMapper();
        AgentDirectory agentDirectory = messageContext.getAgentDirectory();

        var encounterStatementTemplateParameters = EncounterTemplateParameters.builder()
            .encounterStatementId(idMapper.getOrNew(ResourceType.Encounter, encounter.getIdElement()))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForEncounter(encounter))
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTime(encounter.getPeriod().getStartElement()))
            .status(COMPLETE_CODE)
            .components(components)
            .code(buildCode(encounter))
            .displayName(buildDisplayName(encounter))
            .originalText(buildOriginalText(encounter))
            .locationName(buildLocationPertinentInformation(encounter));

        final Optional<String> recReference = findParticipantWithCoding(encounter, ParticipantCoding.RECORDER)
            .map(agentDirectory::getAgentId);
        recReference.map(encounterStatementTemplateParameters::author);

        messageContext.getInputBundleHolder()
            .getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE)
            .filter(ListResource::hasDate)
            .map(ListResource::getDateElement)
            .map(DateFormatUtil::toHl7Format)
            .ifPresent(encounterStatementTemplateParameters::authorTime);

        final Optional<String> pprfReference = findParticipantWithCoding(encounter, ParticipantCoding.PERFORMER)
            .map(agentDirectory::getAgentId);

        encounterStatementTemplateParameters.participant2(pprfReference.orElse(recReference.orElse(null)));

        updateEhrFolderEffectiveTime(encounter);

        return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
            encounterStatementTemplateParameters.build());
    }

    private String buildLocationPertinentInformation(Encounter encounter) {
        if (encounter.hasLocation()) {
            return messageContext
                .getInputBundleHolder()
                .getResource(encounter.getLocationFirstRep().getLocation().getReferenceElement())
                .map(Location.class::cast)
                .map(Location::getName)
                .orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    private void updateEhrFolderEffectiveTime(Encounter encounter) {
        if (encounter.hasPeriod()) {
            messageContext.getEffectiveTime().updateEffectiveTimePeriod(encounter.getPeriod());
        }
    }

    private Optional<Reference> findParticipantWithCoding(Encounter encounter, ParticipantCoding coding) {
        return encounter.getParticipant().stream()
            .filter(EncounterParticipantComponent::hasType)
            .filter(participant -> participant.getType().stream()
                .filter(CodeableConcept::hasCoding)
                .anyMatch(codeableConcept -> codeableConcept.getCoding().stream()
                    .filter(Coding::hasCode)
                    .map(Coding::getCode)
                    .anyMatch(coding.getCoding()::equals)))
            .filter(EncounterParticipantComponent::hasIndividual)
            .map(EncounterParticipantComponent::getIndividual)
            .filter(Reference::hasReference)
            .findAny();
    }

    private boolean isSnomedAndWithinEhrCompositionVocabularyCodes(Coding coding) {
        return coding.hasSystem() && coding.getSystem().equals(SNOMED_SYSTEM)
            && EHR_COMPOSITION_NAME_VOCABULARY_CODES.contains(coding.getCode());
    }

    private CodeableConcept extractType(Encounter encounter) {
        return encounter.getType()
            .stream()
            .findFirst()
            .orElseThrow(() -> new EhrMapperException("Could not map Encounter type"));
    }

    private String buildCode(Encounter encounter) {
        var type = extractType(encounter);
        return type.getCoding()
            .stream()
            .filter(this::isSnomedAndWithinEhrCompositionVocabularyCodes)
            .findFirst()
            .map(Coding::getCode)
            .orElse(OTHER_REPORT_CODE);
    }

    private String buildDisplayName(Encounter encounter) {
        var type = extractType(encounter);
        return getDisplayName(type);
    }

    private String buildOriginalText(Encounter encounter) {
        var type = extractType(encounter);
        if (getDisplayName(type).equals(OTHER_REPORT_DISPLAY)) {
            return type.hasText() ? type.getText() : type.getCodingFirstRep().getDisplay();
        } else if (type.hasText()) {
            return type.getText();
        }
        return StringUtils.EMPTY;
    }

    @SneakyThrows
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
        justification = "https://github.com/spotbugs/spotbugs/issues/1338")
    private static Set<String> getEhrCompositionNameVocabularyCodes() {
        try (InputStream is = EncounterMapper.class.getClassLoader().getResourceAsStream("ehr_composition_name_vocabulary_codes.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            return reader.lines()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toUnmodifiableSet());
        }
    }

    private String getDisplayName(CodeableConcept type) {
        return type.getCoding()
            .stream()
            .filter(this::isSnomedAndWithinEhrCompositionVocabularyCodes)
            .findFirst()
            .map(Coding::getDisplay)
            .orElse(OTHER_REPORT_DISPLAY);
    }
}
