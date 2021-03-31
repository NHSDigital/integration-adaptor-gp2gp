package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ObservationStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class ObservationStatementMapper {
    private static final List<Class<? extends Type>> UNHANDLED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);
    private static final Mustache OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE =
        TemplateUtils.loadTemplate("unstructured_observation_statement_template.mustache");
    private static final String REFERENCE_RANGE_UNIT_PREFIX = "Range Units: ";
    private static final int COMMENT_OFFSET = 0;
    private static final String INTERPRETATION_CODE_SYSTEM = "http://hl7.org/fhir/v2/0078";

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final PertinentInformationObservationValueMapper pertinentInformationObservationValueMapper;
    private final CodeableConceptCdMapper codeableConceptCdMapper;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
            .code(prepareCode(observation))
            .comment(prepareComment(observation))
            .issued(DateFormatUtil.toHl7Format(observation.getIssuedElement()))
            .isNested(isNested)
            .effectiveTime(StatementTimeMappingUtils.prepareEffectiveTimeForObservation(observation));

        if (observation.hasValue()) {
            Type value = observation.getValue();

            if (UNHANDLED_TYPES.contains(value.getClass())) {
                LOGGER.info("Observation value type {} not supported. Mapping for this field is skipped",
                    observation.getValue().getClass());
            } else if (structuredObservationValueMapper.isStructuredValueType(value)) {
                observationStatementTemplateParametersBuilder.value(
                    structuredObservationValueMapper.mapObservationValueToStructuredElement(value));
            }
        }

        if (observation.hasReferenceRange() && observation.hasValueQuantity()) {
            observationStatementTemplateParametersBuilder.referenceRange(
                structuredObservationValueMapper.mapReferenceRangeType(observation.getReferenceRangeFirstRep()));
        }

        if (observation.hasInterpretation()) {
            observation.getInterpretation().getCoding().stream()
                .filter(this::isInterpretationCode)
                .findFirst()
                .ifPresent(coding ->
                    observationStatementTemplateParametersBuilder.interpretation(
                        structuredObservationValueMapper.mapInterpretation(coding))
            );
        }

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE,
            observationStatementTemplateParametersBuilder.build());
    }

    private String prepareComment(Observation observation) {
        StringBuilder commentBuilder = new StringBuilder(observation.hasComment() ? observation.getComment() : StringUtils.EMPTY);

        if (observation.hasValue()  && pertinentInformationObservationValueMapper.isPertinentInformation(observation.getValue())) {
            commentBuilder.insert(COMMENT_OFFSET,
                pertinentInformationObservationValueMapper.mapObservationValueToPertinentInformation(observation.getValue()));
        }

        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent referenceRange = observation.getReferenceRangeFirstRep();

            if (observation.hasValueQuantity()) {
                Optional<String> referenceRangeUnit = extractUnit(referenceRange);

                if (referenceRangeUnit.isPresent() && isRangeUnitValid(referenceRangeUnit.get(), observation.getValueQuantity())) {
                    commentBuilder.insert(COMMENT_OFFSET, REFERENCE_RANGE_UNIT_PREFIX + referenceRangeUnit.get() + StringUtils.SPACE);
                }
            } else {
                commentBuilder.insert(COMMENT_OFFSET,
                    pertinentInformationObservationValueMapper.mapReferenceRangeToPertinentInformation(referenceRange));
            }
        }

        if (observation.hasInterpretation()) {
            CodeableConcept interpretation = observation.getInterpretation();

            boolean isInterpretationCode = interpretation.getCoding().stream()
                .anyMatch(this::isInterpretationCode);

            if (!isInterpretationCode) {
                Optional<Coding> codeWithUserSelected = interpretation.getCoding().stream()
                    .filter(code -> !isInterpretationCode(code) && code.hasUserSelected() && code.getUserSelected())
                    .findFirst();

                Coding coding = codeWithUserSelected.orElse(interpretation.getCodingFirstRep());

                if (coding.hasDisplay()) {
                    commentBuilder.insert(COMMENT_OFFSET, coding.getDisplay() + StringUtils.SPACE);
                }
                if (coding.hasCode()) {
                    commentBuilder.insert(COMMENT_OFFSET, coding.getCode() + StringUtils.SPACE);
                }
                if (coding.hasCode() || coding.hasDisplay()) {
                    commentBuilder.insert(COMMENT_OFFSET, "Interpretation Code: ");
                }

                if (interpretation.hasText()) {
                    commentBuilder.insert(COMMENT_OFFSET, "Interpretation Text: "
                        + interpretation.getText() + StringUtils.SPACE);
                }
            }
        }

        return commentBuilder.toString();
    }

    private Optional<String> extractUnit(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.hasHigh() && referenceRange.getHigh().hasUnit()) {
            return Optional.of(referenceRange.getHigh().getUnit());
        } else if (referenceRange.hasLow() && referenceRange.getLow().hasUnit()) {
            return Optional.of(referenceRange.getLow().getUnit());
        }

        return Optional.empty();
    }

    private String prepareCode(Observation observation) {
        if (observation.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(observation.getCode());
        }
        throw new EhrMapperException("Observation code is not present");
    }

    private boolean isRangeUnitValid(String unit, Quantity quantity) {
        return quantity.hasUnit() && !unit.equals(quantity.getUnit());
    }

    private boolean isInterpretationCode(Coding coding) {
        String codingSystem = coding.getSystem();
        String code = coding.getCode();

        return (coding.hasSystem() && codingSystem.equals(INTERPRETATION_CODE_SYSTEM))
            && Set.of("H", "HH", "HU", "L", "LL", "LU", "A", "AA").contains(code);
    }
}
