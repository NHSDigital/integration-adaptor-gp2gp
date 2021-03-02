package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class EncounterMapper {
    private static final Mustache ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE =
        TemplateUtils.loadTemplate("ehr_encounter_to_ehr_composition_template.mustache");
    private static final String COMPLETE_CODE = "COMPLETE";
    private static final List<String> SNOMED_ENCOUNTER_CODES = List.of(
        "24561000000109",
        "37361000000105",
        "37351000000107",
        "37351000000107",
        "37341000000109",
        "37331000000100",
        "15611000000104",
        "37321000000102",
        "24571000000102",
        "25561000000105",
        "24581000000100",
        "25571000000103",
        "24591000000103",
        "25581000000101",
        "37311000000108",
        "37301000000106",
        "37281000000105",
        "37291000000107",
        "24601000000109",
        "25591000000104",
        "25601000000105",
        "24611000000106",
        "25611000000107",
        "24621000000100",
        "25631000000104",
        "25621000000101",
        "25641000000108",
        "24631000000103",
        "25651000000106",
        "24641000000107",
        "25661000000109",
        "24651000000105",
        "24661000000108",
        "25671000000102",
        "25681000000100",
        "25691000000103",
        "25701000000103",
        "24671000000101",
        "25711000000101",
        "24681000000104",
        "24691000000102",
        "24701000000102",
        "25741000000100",
        "25731000000109",
        "37271000000108",
        "24711000000100",
        "24721000000106",
        "24881000000103",
        "25751000000102",
        "25761000000104",
        "24731000000108",
        "25771000000106",
        "25791000000105",
        "25781000000108",
        "24741000000104",
        "25801000000109",
        "109341000000100",
        "24751000000101",
        "25811000000106"
    );

    private final MessageContext messageContext;
    private final EncounterComponentsMapper encounterComponentsMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;

    public String mapEncounterToEhrComposition(Encounter encounter) {
        String components = encounterComponentsMapper.mapComponents(encounter);

        var encounterStatementTemplateParameters = EncounterTemplateParameters.builder()
            .encounterStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Encounter, encounter.getId()))
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForEncounter(encounter))
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForEncounter(encounter))
            .status(COMPLETE_CODE)
            .type(buildType(encounter))
            .components(components);

        return TemplateUtils.fillTemplate(ENCOUNTER_STATEMENT_TO_EHR_COMPOSITION_TEMPLATE,
            encounterStatementTemplateParameters.build());
    }

    private String buildType(Encounter encounter) {
        if (encounter.hasType() && containsSnomedEncounterCode(encounter)) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(encounter.getTypeFirstRep());
        } else if(encounter.hasType() && !containsSnomedEncounterCode(encounter)) {
            return codeableConceptCdMapper.mapEncounterTypeNoSnomed(encounter);
        }
        return StringUtils.EMPTY;
    }

    private boolean containsSnomedEncounterCode(Encounter encounter) {
        if (encounter.getTypeFirstRep().hasCoding()) {
            var code = Optional.of(encounter.getTypeFirstRep().getCodingFirstRep().getCode());
            return code.map(SNOMED_ENCOUNTER_CODES::contains).orElse(false);
        }
        return false;
    }
}
