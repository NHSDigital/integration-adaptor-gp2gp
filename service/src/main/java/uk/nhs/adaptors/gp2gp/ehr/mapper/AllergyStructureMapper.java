package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractOnsetDate;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractReaction;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor.extractAssertedDate;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toTextFormat;
import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AllergyStructureTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AllergyStructureTemplateParameters.AllergyStructureTemplateParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AllergyStructureMapper {
    private static final Mustache ALLERGY_STRUCTURE_TEMPLATE = TemplateUtils.loadTemplate("ehr_allergy_structure_template.mustache");

    private static final String ALLERGY_INTOLERANCE_END_URL =
        "https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-AllergyIntoleranceEnd-1";
    private static final String END_DATE = "End Date: ";
    private static final String STATUS = "Status: ";
    private static final String TYPE = "Type: ";
    private static final String CRITICALITY = "Criticality: ";
    private static final String PATIENT_ASSERTER = "Asserted By Patient";
    private static final String RELATED_PERSON_ASSERTER = "Asserted By: Related Person";
    private static final String LAST_OCCURRENCE = "Last Occurred: ";
    private static final String PATIENT_RECORDER = "Recorded By Patient";
    private static final String ENVIRONMENT_CATEGORY = "environment";
    private static final String MEDICATION_CATEGORY = "medication";
    private static final String RESOLVED_CLINICAL_STATUS = "resolved";
    private static final String UNSPECIFIED_ALLERGY_CODE = "<code code=\"SN53.00\" codeSystem=\"2.16.840.1.113883.2.1.6.2\" "
        + "displayName=\"Allergy, unspecified\"/>";
    private static final String DRUG_ALLERGY_CODE = "<code code=\"14L..00\" codeSystem=\"2.16.840.1.113883.2.1.6.2\" displayName=\"H/O: "
        + "drug allergy\"/>";
    private static final String COMMA = ", ";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;
    private final ConfidentialityService confidentialityService;

    public String mapAllergyIntoleranceToAllergyStructure(AllergyIntolerance allergyIntolerance) {
        final IdMapper idMapper = messageContext.getIdMapper();

        var confidentialityCode = confidentialityService.generateConfidentialityCode(allergyIntolerance);

        var allergyStructureTemplateParameters = AllergyStructureTemplateParameters.builder()
            .allergyStructureId(idMapper.getOrNew(ResourceType.AllergyIntolerance, allergyIntolerance.getIdElement()))
            .observationId(idMapper.getOrNew(ResourceType.Observation, allergyIntolerance.getIdElement()))
            .pertinentInformation(buildPertinentInformation(allergyIntolerance))
            .code(buildCode(allergyIntolerance))
            .effectiveTime(buildEffectiveTime(allergyIntolerance))
            .availabilityTime(buildAvailabilityTime(allergyIntolerance))
            .confidentialityCode(confidentialityCode.orElse(null));

        buildCategory(allergyIntolerance, allergyStructureTemplateParameters);

        if (allergyIntolerance.hasRecorder()) {
            buildParticipant(allergyIntolerance.getRecorder(), ParticipantType.AUTHOR)
                .ifPresent(allergyStructureTemplateParameters::author);
        }

        buildAuthor(allergyIntolerance, allergyStructureTemplateParameters);

        return TemplateUtils.fillTemplate(ALLERGY_STRUCTURE_TEMPLATE, allergyStructureTemplateParameters.build());
    }

    private String buildPertinentInformation(AllergyIntolerance allergyIntolerance) {
        List<String> descriptionList = retrievePertinentInformation(allergyIntolerance);

        return descriptionList
            .stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String buildCode(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasClinicalStatus()) {
            if (RESOLVED_CLINICAL_STATUS.equals(allergyIntolerance.getClinicalStatus().toCode())
                && allergyIntolerance.hasCode()) {

                var category = allergyIntolerance.getCategory()
                    .stream()
                    .map(PrimitiveType::getValueAsString)
                    .filter(value -> value.equals(ENVIRONMENT_CATEGORY) || value.equals(MEDICATION_CATEGORY))
                    .findFirst()
                    .orElse(StringUtils.EMPTY);

                if (category.equals(ENVIRONMENT_CATEGORY)) {
                    return codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(allergyIntolerance.getCode(),
                        allergyIntolerance.getClinicalStatus());
                } else if (category.equals(MEDICATION_CATEGORY)) {
                    return codeableConceptCdMapper.mapToNullFlavorCodeableConceptForAllergy(allergyIntolerance.getCode(),
                        allergyIntolerance.getClinicalStatus());
                } else {
                    throw new EhrMapperException("Category could not be mapped");
                }
            }
            if (allergyIntolerance.hasCode()) {
                return codeableConceptCdMapper.mapCodeableConceptToCdForAllergy(allergyIntolerance.getCode(),
                    allergyIntolerance.getClinicalStatus());
            }
        }
        throw new EhrMapperException("Allergy code not present");
    }

    private String buildAvailabilityTime(AllergyIntolerance allergyIntolerance) {

        var availabilityTime = extractAssertedDate(allergyIntolerance);

        return StatementTimeMappingUtils.prepareAvailabilityTimeForAllergyIntolerance(availabilityTime);
    }

    private String buildEffectiveTime(AllergyIntolerance allergyIntolerance) {
        var onsetDate = extractOnsetDate(allergyIntolerance);

        return StatementTimeMappingUtils.prepareEffectiveTimeForAllergyIntolerance(onsetDate);
    }

    private void buildCategory(AllergyIntolerance allergyIntolerance, AllergyStructureTemplateParametersBuilder templateParameters) {
        var category = allergyIntolerance.getCategory()
            .stream()
            .map(PrimitiveType::getValueAsString)
            .filter(value -> value.equals(ENVIRONMENT_CATEGORY) || value.equals(MEDICATION_CATEGORY))
            .findFirst()
            .orElse(StringUtils.EMPTY);

        if (category.equals(ENVIRONMENT_CATEGORY)) {
            templateParameters.categoryCode(UNSPECIFIED_ALLERGY_CODE);
        } else if (category.equals(MEDICATION_CATEGORY)) {
            templateParameters.categoryCode(DRUG_ALLERGY_CODE);
        } else {
            throw new EhrMapperException("Category could not be mapped");
        }
    }

    private Optional<String> buildParticipant(Reference reference, ParticipantType participantType) {
        if (reference.getReferenceElement().getResourceType().startsWith(ResourceType.Practitioner.name())) {
            var authorReferenceId = messageContext.getAgentDirectory().getAgentId(reference);
            return Optional.of(participantMapper.mapToParticipant(authorReferenceId, participantType));
        }

        return Optional.empty();
    }

    private void buildAuthor(AllergyIntolerance allergyIntolerance, AllergyStructureTemplateParametersBuilder templateParameter) {
        if (isValidAsserter(allergyIntolerance)) {
            buildParticipant(allergyIntolerance.getAsserter(), ParticipantType.PERFORMER)
                .ifPresent(templateParameter::performer);
        } else if (allergyIntolerance.hasRecorder()) {
            buildParticipant(allergyIntolerance.getRecorder(), ParticipantType.PERFORMER)
                .ifPresent(templateParameter::performer);
        }
    }

    private List<String> retrievePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return List.of(
            buildExtensionReasonEndPertinentInformation(allergyIntolerance),
            buildClinicalStatusPertinentInformation(allergyIntolerance),
            buildTypePertinentInformation(allergyIntolerance),
            buildCriticalityPertinentInformation(allergyIntolerance),
            buildAsserterPertinentInformation(allergyIntolerance),
            buildLastOccurrencePertinentInformation(allergyIntolerance),
            buildRecorderPertinentInformation(allergyIntolerance),
            buildReactionPertinentInformation(allergyIntolerance),
            buildNotePertinentInformation(allergyIntolerance),
            buildEndDatePertinentInformation(allergyIntolerance)
        );
    }

    private boolean isValidAsserter(AllergyIntolerance allergyIntolerance) {
        return allergyIntolerance.hasAsserter()
            && allergyIntolerance.getAsserter().getReferenceElement().getResourceType().startsWith(ResourceType.Practitioner.name());
    }

    private String buildExtensionReasonEndPertinentInformation(AllergyIntolerance allergyIntolerance) {
        return filterExtensionByUrl(allergyIntolerance, ALLERGY_INTOLERANCE_END_URL)
            .map(AllergyStructureExtractor::extractReasonEnd)
            .orElse(StringUtils.EMPTY);
    }

    private String buildClinicalStatusPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasClinicalStatus()) {
            if (allergyIntolerance.hasNote()) {
                return STATUS + StringUtils.capitalize(allergyIntolerance.getClinicalStatus().toCode());
            }
            return STATUS + allergyIntolerance.getClinicalStatus().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildTypePertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasType()) {
            return TYPE + allergyIntolerance.getType().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildCriticalityPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasCriticality()) {
            return CRITICALITY + allergyIntolerance.getCriticality().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildAsserterPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasAsserter()) {
            IIdType reference = allergyIntolerance.getAsserter().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_ASSERTER;
            } else if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext
                    .getInputBundleHolder()
                    .getResource(reference)
                    .map(RelatedPerson.class::cast)
                    .filter(RelatedPerson::hasName)
                    .map(RelatedPerson::getName)
                    .filter(names -> !names.isEmpty())
                    .map(List::getFirst)
                    .map(name -> Optional.ofNullable(name.getText())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(name::getNameAsSingleString))
                    .filter(StringUtils::isNotBlank)
                    .map(name -> RELATED_PERSON_ASSERTER + StringUtils.SPACE + name)
                    .orElse(RELATED_PERSON_ASSERTER);
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildLastOccurrencePertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasLastOccurrence()) {
            return LAST_OCCURRENCE + toTextFormat(allergyIntolerance.getLastOccurrenceElement());
        }
        return StringUtils.EMPTY;
    }

    private String buildRecorderPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasRecorder()) {
            IIdType reference = allergyIntolerance.getRecorder().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_RECORDER;
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildReactionPertinentInformation(AllergyIntolerance allergyIntolerance) {
        AtomicInteger reactionCount = new AtomicInteger(1);
        if (allergyIntolerance.hasReaction()) {
            return allergyIntolerance.getReaction()
                .stream()
                .map(reaction -> extractReaction(reaction, reactionCount))
                .collect(Collectors.joining(COMMA));
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return Stream.concat(
            messageContext.getInputBundleHolder().getRelatedConditions(allergyIntolerance.getId())
                .stream()
                .map(Condition::getNote)
                .flatMap(List::stream),
            allergyIntolerance.hasNote() ? allergyIntolerance.getNote().stream() : Stream.empty()
        )
            .map(Annotation::getText)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String buildEndDatePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return filterExtensionByUrl(allergyIntolerance, ALLERGY_INTOLERANCE_END_URL)
            .map(extension -> AllergyStructureExtractor.extractEndDate(extension, DateFormatUtil::toTextFormat))
            .filter(StringUtils::isNotBlank)
            .map(endDate -> END_DATE + endDate)
            .orElse(StringUtils.EMPTY);
    }
}
