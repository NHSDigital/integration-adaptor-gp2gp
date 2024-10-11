package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;
import static uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor.extractObservation;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DiaryPlanStatementMapper.PlanStatementMapperParameters.PlanStatementMapperParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DiaryPlanStatementMapper {

    public static final String REASON_CODE_TEXT_FORMAT = "Reason Codes: %s";
    public static final String EARLIEST_RECALL_DATE_FORMAT = "Earliest Recall Date: %s";
    public static final String RECALL_DEVICE = "Recall Device: %s %s";
    public static final String RECALL_ORGANISATION = "Recall Organisation: %s";
    private static final Mustache PLAN_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_plan_statement_template.mustache");
    private static final String EMPTY_DATE = "nullFlavor=\"UNK\"";
    private static final String FULL_DATE = "value=\"%s\"";
    private static final String REASON_CODE_SEPARATOR = ", ";
    private static final String COMMA = ", ";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapProcedureRequestToPlanStatement(ProcedureRequest procedureRequest, Boolean isNested) {
        if (procedureRequest.getIntent() == ProcedureRequest.ProcedureRequestIntent.PLAN) {
            return mapDiaryEntryToPlanStatement(procedureRequest, isNested);
        }

        return null;
    }

    private String mapDiaryEntryToPlanStatement(ProcedureRequest procedureRequest, Boolean isNested) {
        var idMapper = messageContext.getIdMapper();
        var availabilityTime = buildAvailabilityTime(procedureRequest);
        PlanStatementMapperParametersBuilder builder = PlanStatementMapperParameters.builder()
            .isNested(isNested)
            .id(idMapper.getOrNew(ResourceType.ProcedureRequest, procedureRequest.getIdElement()))
            .availabilityTime(availabilityTime)
            .effectiveTime(buildEffectiveTime(procedureRequest))
            .text(buildText(procedureRequest))
            .code(buildCode(procedureRequest));

        buildParticipant(procedureRequest).ifPresent(builder::participant);

        return TemplateUtils.fillTemplate(PLAN_STATEMENT_TEMPLATE, builder.build());
    }

    private String buildAvailabilityTime(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasAuthoredOn()) {
            return DateFormatUtil.toHl7Format(procedureRequest.getAuthoredOnElement());
        }

        throw new EhrMapperException(
            String.format("ProcedureRequest: %s does not have required authoredOn", procedureRequest.getId())
        );
    }

    private String buildEffectiveTime(ProcedureRequest procedureRequest) {
        DateTimeType date = null;
        if (procedureRequest.hasOccurrenceDateTimeType()) {
            date = procedureRequest.getOccurrenceDateTimeType();
        } else if (procedureRequest.hasOccurrencePeriod()) {
            Period occurrencePeriod = procedureRequest.getOccurrencePeriod();
            date = occurrencePeriod.hasEnd() ? occurrencePeriod.getEndElement() : occurrencePeriod.getStartElement();
        }

        return formatEffectiveDate(date);
    }

    private String buildText(ProcedureRequest procedureRequest) {
        return Stream.of(
            getSupportingInformation(procedureRequest),
            getEarliestRecallDate(procedureRequest),
            getRequester(procedureRequest),
            getReasonCode(procedureRequest),
            getNotes(procedureRequest)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private String buildCode(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(procedureRequest.getCode());
        }
        throw new EhrMapperException("Procedure request code not present");
    }

    private Optional<String> buildParticipant(ProcedureRequest procedureRequest) {
        var requesterAgent = procedureRequest.getRequester().getAgent();

        if (requesterAgent.hasReference()) {
            var resourceType = requesterAgent.getReference().split("/")[0];
            if (resourceType.equals(ResourceType.Practitioner.name())) {
                String participantId = messageContext.getAgentDirectory().getAgentId(requesterAgent);
                String participant = participantMapper.mapToParticipant(participantId, ParticipantType.PERFORMER);
                return Optional.of(participant);
            }
        }

        return Optional.empty();
    }

    private String formatEffectiveDate(DateTimeType date) {
        return date != null ? String.format(FULL_DATE, DateFormatUtil.toHl7Format(date)) : EMPTY_DATE;
    }

    private Optional<String> getSupportingInformation(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasSupportingInfo()) {
            return Optional.of("Supporting Information: " + procedureRequest.getSupportingInfo().stream()
                .filter(this::checkIfReferenceIsObservation)
                .map((observationReference) -> extractObservation(messageContext, observationReference))
                .collect(Collectors.joining(COMMA)));
        }
        return Optional.empty();
    }

    private Optional<String> getEarliestRecallDate(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasOccurrencePeriod() && procedureRequest.getOccurrencePeriod().hasEnd()) {
            return Optional.of(formatStartDate(procedureRequest));
        }

        return Optional.empty();
    }


    private Optional<String> getRequester(ProcedureRequest procedureRequest) {
        Reference agent = procedureRequest.getRequester().getAgent();

        if (agent.hasReference()) {
            IIdType reference = agent.getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Organization.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(Organization.class::cast)
                    .map(this::formatOrganization);
            } else if (reference.getResourceType().equals(ResourceType.Device.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(Device.class::cast)
                    .map(this::formatDevice);
            }
        }

        return Optional.empty();
    }

    private Optional<String> getReasonCode(ProcedureRequest procedureRequest) {
        var reasons = procedureRequest.getReasonCode()
            .stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(REASON_CODE_SEPARATOR));

        return reasons.isBlank() ? Optional.empty() : Optional.of(formatReason(reasons));
    }

    private Optional<String> getNotes(ProcedureRequest procedureRequest) {
        var notes = procedureRequest.getNote()
            .stream()
            .map(Annotation::getText)
            .collect(Collectors.joining(StringUtils.SPACE));

        return notes.isEmpty() ? Optional.empty() : Optional.of(notes);
    }

    private boolean checkIfReferenceIsObservation(Reference reference) {
        return ResourceType.Observation.name().equals(reference.getReferenceElement().getResourceType());
    }

    private String formatStartDate(ProcedureRequest procedureRequest) {
        return String.format(EARLIEST_RECALL_DATE_FORMAT,
            DateFormatUtil.toTextFormat(procedureRequest.getOccurrencePeriod().getStartElement()));
    }

    private String formatOrganization(Organization organization) {
        return String.format(RECALL_ORGANISATION, organization.getName());
    }

    private String formatDevice(Device device) {
        return RECALL_DEVICE.formatted(
            extractTextOrCoding(device.getType()).orElse(StringUtils.EMPTY),
            device.hasManufacturer() ? device.getManufacturer() : StringUtils.EMPTY
        ).stripTrailing();
    }

    private String formatReason(String value) {
        return String.format(REASON_CODE_TEXT_FORMAT, value);
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
        private String code;
        private String participant;
    }
}