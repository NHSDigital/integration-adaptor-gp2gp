package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ResourceType;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RequestStatementMapper {
    private static final Mustache REQUEST_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_request_statement_template.mustache");

    private static final String PRIORITY = "PRIORITY: ";
    private static final String SERVICE_REQUESTED = "Service(s) Requested: ";
    private static final String SPECIALTY = "Specialty: ";
    private static final String UBRN_SYSTEM_URL = "https://fhir.nhs.uk/Id/ubr-number";
    private static final String UBRN = "UBRN: ";
    private static final String REASON_CODE = "Reason Codes: ";
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String NOTE_AUTHOR = "Author: ";
    private static final String NOTE_AUTHOR_RELATION = NOTE_AUTHOR + "Relation ";
    private static final String NOTE_AUTHOR_PRACTITIONER= NOTE_AUTHOR + "Practitioner ";
    private static final String NOTE_AUTHOR_PATIENT= NOTE_AUTHOR + "Patient";
    private static final String COMMA = ",";

    private final MessageContext messageContext;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, boolean isNested) {
        var requestStatementTemplateParameters = RequestStatementTemplateParameters.builder()
            .requestStatementId(messageContext.getIdMapper().getOrNew(ResourceType.ReferralRequest, referralRequest.getId()))
            .isNested(isNested)
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForReferralRequest(referralRequest))
            .description(buildDescription(referralRequest))
            .build();

        if (!referralRequest.hasReasonCode()) {
            requestStatementTemplateParameters.setDefaultReasonCode(DEFAULT_REASON_CODE_XML);
        }

        return TemplateUtils.fillTemplate(REQUEST_STATEMENT_TEMPLATE, requestStatementTemplateParameters);
    }

    private String buildDescription(ReferralRequest referralRequest) {
        List<String> descriptionList = retrieveDescription(referralRequest);
        return descriptionList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrieveDescription(ReferralRequest referralRequest) {
        return List.of(
            buildIdentifierDescription(referralRequest),
            buildPriorityDescription(referralRequest),
            buildServiceRequestedDescription(referralRequest),
            buildRequesterDescription(referralRequest),
            buildSpecialtyDescription(referralRequest),
            buildRecipientDescription(referralRequest),
            buildReasonCodeDescription(referralRequest),
            buildNoteDescription(referralRequest),
            buildTextDescription(referralRequest)
        );
    }

    private String buildIdentifierDescription(ReferralRequest referralRequest) {
        Optional<String> identifier = Optional.empty();
        if (referralRequest.hasIdentifier()) {
            identifier = referralRequest.getIdentifier()
                .stream()
                .filter(val -> val.getSystem().equals(UBRN_SYSTEM_URL))
                .map(Identifier::getValue)
                .findFirst();
        }
        return identifier.map(value -> UBRN + value).orElse(StringUtils.EMPTY);
    }

    private String buildPriorityDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasPriority()) {
            return PRIORITY + referralRequest.getPriority().getDisplay();
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String buildServiceRequestedDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasServiceRequested()) {
            return SERVICE_REQUESTED + extractServiceRequestedString(referralRequest);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String buildRequesterDescription(ReferralRequest referralRequest) {
        return StringUtils.EMPTY;
    }

    private String extractServiceRequestedString(ReferralRequest referralRequest) {
        return referralRequest.getServiceRequested().stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildSpecialtyDescription(ReferralRequest referralRequest) {
        Optional<String> specialty = Optional.empty();
        if (referralRequest.hasSpecialty()) {
            CodeableConcept specialtyObject = referralRequest.getSpecialty();
            specialty = CodeableConceptMappingUtils.extractTextOrCoding(specialtyObject);
        }
        return specialty.map(value -> SPECIALTY + value).orElse(StringUtils.EMPTY);
    }

    private String buildRecipientDescription(ReferralRequest referralRequest) {
        return StringUtils.EMPTY;
    }

    private String buildReasonCodeDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasReasonCode() && (referralRequest.getReasonCode().size() > 1)) {
            return REASON_CODE + extractReasonCodeString(referralRequest);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String extractReasonCodeString(ReferralRequest referralRequest) {
        var ignoreFirstReasonCode = referralRequest.getReasonCode().subList(1, referralRequest.getReasonCode().size());
        return ignoreFirstReasonCode.stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildNoteDescription(ReferralRequest referralRequest) {
        return StringUtils.EMPTY;
    }

    private String buildTextDescription(ReferralRequest referralRequest) {
        Optional<String> text = Optional.ofNullable(referralRequest.getDescription());
        return text.orElse(StringUtils.EMPTY);
    }
}
