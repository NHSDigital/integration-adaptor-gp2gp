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
    private static final String UBRN_SYSTEM_URL = "https://fhir.nhs.uk/Id/ubr-numbe";
    private static final String UBRN = "UBRN: ";
    private static final String COMMA = ",";

    private final MessageContext messageContext;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, boolean isNested) {
        var requestStatementTemplateParameters = RequestStatementTemplateParameters.builder()
                .requestStatementId(messageContext.getIdMapper().getOrNew(ResourceType.ReferralRequest, referralRequest.getId()))
                .isNested(isNested)
                .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForReferralRequest(referralRequest))
                .description(buildDescription(referralRequest))
                .build();

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
            buildSpecialtyDescription(referralRequest),
            buildIdentifierDescription(referralRequest),
            buildServiceRequestedString(referralRequest),
            buildPriorityDescription(referralRequest),
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

    private String buildSpecialtyDescription(ReferralRequest referralRequest) {
        Optional<String> specialty = Optional.empty();
        if (referralRequest.hasSpecialty()) {
            CodeableConcept specialtyObject = referralRequest.getSpecialty();
            specialty = CodeableConceptMappingUtils.extractTextOrCoding(specialtyObject);
        }
        return specialty.map(value -> SPECIALTY + value).orElse(StringUtils.EMPTY);
    }

    private String buildServiceRequestedString(ReferralRequest referralRequest) {
        if (referralRequest.hasServiceRequested()) {
            return SERVICE_REQUESTED + extractServiceRequestedString(referralRequest);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String extractServiceRequestedString(ReferralRequest referralRequest) {
        return referralRequest.getServiceRequested().stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildPriorityDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasPriority()) {
            return PRIORITY + referralRequest.getPriority().getDisplay();
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String buildTextDescription(ReferralRequest referralRequest) {
        Optional<String> text = Optional.ofNullable(referralRequest.getDescription());
        return text.orElse(StringUtils.EMPTY);
    }
}
