package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
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

    private final MessageContext messageContext;
    private final StructuredObservationValueMapper structuredObservationValueMapper;
    private final PertinentInformationObservationValueMapper pertinentInformationObservationValueMapper;

    public String mapObservationToObservationStatement(Observation observation, boolean isNested) {
        var observationStatementTemplateParametersBuilder = ObservationStatementTemplateParameters.builder()
            .observationStatementId(messageContext.getIdMapper().getOrNew(ResourceType.Observation, observation.getId()))
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

        return TemplateUtils.fillTemplate(OBSERVATION_STATEMENT_EFFECTIVE_TIME_TEMPLATE,
            observationStatementTemplateParametersBuilder.build());
    }

    private String prepareComment(Observation observation) {
        StringBuilder commentBuilder = new StringBuilder(observation.hasComment() ? observation.getComment() : StringUtils.EMPTY);

        if (observation.hasValue() && pertinentInformationObservationValueMapper.isPertinentInformation(observation.getValue())) {
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

    private boolean isRangeUnitValid(String unit, Quantity quantity) {
        return quantity.hasUnit() && !unit.equals(quantity.getUnit());
    }
}
