package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Date;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DiaryPlanStatementMapper {

    private static final Mustache PLAN_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_plan_statement_template.mustache");
    private static final String COMMA = ", ";
    private static final String EMPTY_DATE = "nullFlavor=\"UNK\"";
    private static final String FULL_DATE = "value=\"%s\"";

    private final MessageContext messageContext;

    public String mapDiaryProcedureRequestToPlanStatement(ProcedureRequest procedureRequest, Boolean isNested) {
        if (procedureRequest.getIntent() != ProcedureRequest.ProcedureRequestIntent.PLAN) {
            return StringUtils.EMPTY;
        }

        PlanStatementMapperParameters.PlanStatementMapperParametersBuilder builder = PlanStatementMapperParameters.builder()
            .isNested(isNested)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.ProcedureRequest, procedureRequest.getId()))
            .availabilityTime(buildAvailabilityTime(procedureRequest));

        buildEffectiveTime(procedureRequest).map(builder::effectiveTime);
        buildText(procedureRequest).map(builder::text);

        return TemplateUtils.fillTemplate(PLAN_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildEffectiveTime(ProcedureRequest procedureRequest) {
        Date date = null;
        if (procedureRequest.hasOccurrenceDateTimeType()) {
            date = procedureRequest.getOccurrenceDateTimeType().getValue();
        } else if (procedureRequest.hasOccurrencePeriod()) {
            Period occurrencePeriod = procedureRequest.getOccurrencePeriod();
            date = occurrencePeriod.hasEnd() ? occurrencePeriod.getEnd() : occurrencePeriod.getStart();
        }

        return Optional.of(formatEffectiveDate(date));
    }

    private String formatEffectiveDate(Date date) {
        return date != null ? String.format(FULL_DATE, DateFormatUtil.formatDate(date)) : EMPTY_DATE;
    }

    private String buildAvailabilityTime(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasAuthoredOn()) {
            return DateFormatUtil.formatDate(procedureRequest.getAuthoredOn());
        }

        throw new EhrMapperException(
            String.format("ProcedureRequest: %s does not have required authoredOn", procedureRequest.getId())
        );
    }

    private Optional<String> buildText(ProcedureRequest procedureRequest) {
        return Stream.of(
            getRequester(procedureRequest),
            getReasonCode(procedureRequest),
            getNotes(procedureRequest)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(joining());
    }

    private Optional<String> getReasonCode(ProcedureRequest procedureRequest) {
        return procedureRequest.getReasonCode()
            .stream()
            .map(reasonCode -> {
                if (StringUtils.isNoneBlank(reasonCode.getText())) {
                    return reasonCode.getText();
                }
                return getDisplay(reasonCode);
            })
            .filter(StringUtils::isNoneBlank)
            .reduce(joining());
    }

    private String getDisplay(org.hl7.fhir.dstu3.model.CodeableConcept reasonCode) {
        return reasonCode.getCoding()
            .stream()
            .map(Coding::getDisplay)
            .reduce(joining()).orElse(StringUtils.EMPTY);
    }

    private Optional<String> getNotes(ProcedureRequest procedureRequest) {
        return procedureRequest.getNote()
            .stream()
            .map(Annotation::getText)
            .reduce(joining());
    }

    private BinaryOperator<String> joining() {
        return (value1, value2) -> value1 + COMMA + value2;
    }

    private Optional<String> getRequester(ProcedureRequest procedureRequest) {
        Reference agent = procedureRequest.getRequester().getAgent();

        if (agent.hasReference()) {
            String reference = agent.getReference();
            if (reference.startsWith(ResourceType.Organization.name())
                || reference.startsWith(ResourceType.Device.name())) {
                return Optional.of(agent.getDisplay());
            }
        }

        return Optional.empty();
    }

    @Getter
    @Setter
    @Builder
    public static class PlanStatementMapperParameters {
        private Boolean isNested;
        private String id;
        private String text;
        private String availabilityTime;
        private String effectiveTime;
    }
}
