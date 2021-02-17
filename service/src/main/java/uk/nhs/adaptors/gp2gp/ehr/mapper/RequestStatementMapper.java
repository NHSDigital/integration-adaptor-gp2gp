package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.ExtractBundleResourceUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RequestStatementMapper {
    private static final Mustache REQUEST_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_request_statement_template.mustache");

    private static final String UBRN = "UBRN: ";
    private static final String UBRN_SYSTEM_URL = "https://fhir.nhs.uk/Id/ubr-number";
    private static final String PRIORITY = "Priority: ";
    private static final String SERVICE_REQUESTED = "Service(s) Requested: ";
    private static final String REQUESTER_DEVICE = "Requester Device: ";
    private static final String REQUESTER_ORG = "Requester Org: ";
    private static final String REQUESTER_PATIENT = "Requester: Patient";
    private static final String REQUESTER_RELATION = "Requester: Relation ";
    private static final String SPECIALTY = "Specialty: ";
    private static final String RECIPIENT_PRACTITIONER = "Recipient Practitioner: %s %s";
    private static final String RECIPIENT_HEALTH_CARE_SERVICE = "Recipient HealthCare Service: ";
    private static final String RECIPIENT_ORG = "Recipient Org: ";
    private static final String REASON_CODE = "Reason Codes: ";
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String NOTE_AUTHOR = "Author: ";
    private static final String NOTE_AUTHOR_RELATION = NOTE_AUTHOR + "Relation ";
    private static final String NOTE_AUTHOR_PRACTITIONER = NOTE_AUTHOR + "Practitioner %s %s";
    private static final String NOTE_AUTHOR_PATIENT = NOTE_AUTHOR + "Patient";
    private static final String COMMA = ",";

    private final MessageContext messageContext;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, Bundle bundle, boolean isNested) {
        var requestStatementTemplateParameters = RequestStatementTemplateParameters.builder()
            .requestStatementId(messageContext.getIdMapper().getOrNew(ResourceType.ReferralRequest, referralRequest.getId()))
            .isNested(isNested)
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForReferralRequest(referralRequest))
            .description(buildDescription(referralRequest, bundle))
            .build();

        if (!referralRequest.hasReasonCode()) {
            requestStatementTemplateParameters.setDefaultReasonCode(DEFAULT_REASON_CODE_XML);
        }

        return TemplateUtils.fillTemplate(REQUEST_STATEMENT_TEMPLATE, requestStatementTemplateParameters);
    }

    private String buildDescription(ReferralRequest referralRequest, Bundle bundle) {
        List<String> descriptionList = retrieveDescription(referralRequest, bundle);
        return descriptionList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrieveDescription(ReferralRequest referralRequest, Bundle bundle) {
        return List.of(
            buildIdentifierDescription(referralRequest),
            buildPriorityDescription(referralRequest),
            buildServiceRequestedDescription(referralRequest),
            buildRequesterDescription(referralRequest, bundle),
            buildSpecialtyDescription(referralRequest),
            buildRecipientDescription(referralRequest, bundle),
            buildReasonCodeDescription(referralRequest),
            buildNoteDescription(referralRequest, bundle),
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
        }
        return StringUtils.EMPTY;
    }

    private String buildServiceRequestedDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasServiceRequested()) {
            return SERVICE_REQUESTED + extractServiceRequestedString(referralRequest);
        }
        return StringUtils.EMPTY;
    }

    private String extractServiceRequestedString(ReferralRequest referralRequest) {
        return referralRequest.getServiceRequested().stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildRequesterDescription(ReferralRequest referralRequest, Bundle bundle) {
        if (referralRequest.hasRequester() && referralRequest.getRequester().getAgent().hasReference()) {
            String reference = referralRequest.getRequester().getAgent().getReference();
            if (reference.startsWith(ResourceType.Device.name())) {
                var device = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, reference).map(value -> (Device) value).or(Optional::empty);
                return device.map(value -> CodeableConceptMappingUtils.extractTextOrCoding(value.getType()).map(text -> REQUESTER_DEVICE + text).orElse(StringUtils.EMPTY)).orElse(StringUtils.EMPTY);
            }
            else if (reference.startsWith(ResourceType.Organization.name())) {
                var organization = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, reference).map(value -> (Organization) value).or(Optional::empty);
                return organization.map(value -> REQUESTER_ORG + value.getName()).orElse(StringUtils.EMPTY);
            }
            else if (reference.startsWith(ResourceType.Patient.name())) {
                return REQUESTER_PATIENT;
            }
            else if (reference.startsWith(ResourceType.RelatedPerson.name())) {
                var relatedPerson = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, reference).map(value -> (RelatedPerson) value).or(Optional::empty);
                return relatedPerson.map(value -> REQUESTER_RELATION + value.getNameFirstRep().getText()).orElse(StringUtils.EMPTY);
            }
        }

        return StringUtils.EMPTY;
    }

    private String buildSpecialtyDescription(ReferralRequest referralRequest) {
        Optional<String> specialty = Optional.empty();
        if (referralRequest.hasSpecialty()) {
            CodeableConcept specialtyObject = referralRequest.getSpecialty();
            specialty = CodeableConceptMappingUtils.extractTextOrCoding(specialtyObject);
        }
        return specialty.map(value -> SPECIALTY + value).orElse(StringUtils.EMPTY);
    }

    private String buildRecipientDescription(ReferralRequest referralRequest, Bundle bundle) {
        if (referralRequest.hasRecipient()) {
            var ignoreFirstPractitioner = removeFirstPractitionerReference(referralRequest.getRecipient());

            return ignoreFirstPractitioner.stream()
                .map(value -> extractRecipientString(value, bundle))
                .collect(Collectors.joining(COMMA));
        }

        return StringUtils.EMPTY;
    }

    private List<Reference> removeFirstPractitionerReference(List<Reference> referenceList) {
        for (int i=0; i < referenceList.size()-1; i++) {
            if (referenceList.get(i).getReference().startsWith(ResourceType.Practitioner.name())) {
                // Call practitioner mapper here with reference to first practitioner in reference list
                var practitioner = referenceList.remove(i);
                return referenceList;
            }
        }
        return referenceList;
    }

    private String extractRecipientString(Reference reference, Bundle bundle) {
        String referenceString = reference.getReference();

        if (referenceString.startsWith(ResourceType.Practitioner.name())) {
            var practitioner = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, referenceString).map(value -> (Practitioner) value).or(Optional::empty);
            return practitioner.map(value -> String.format(RECIPIENT_PRACTITIONER, value.getNameFirstRep().getGivenAsSingleString(), value.getNameFirstRep().getFamily())).orElse(StringUtils.EMPTY);
        }
        else if (referenceString.startsWith(ResourceType.HealthcareService.name())) {
            var healthCareService = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, referenceString).map(value -> (HealthcareService) value).or(Optional::empty);
            return healthCareService.map(value -> RECIPIENT_HEALTH_CARE_SERVICE + value.getName()).orElse(StringUtils.EMPTY);
        }
        else if (referenceString.startsWith(ResourceType.Organization.name())) {
            var organization = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, referenceString).map(value -> (Organization) value).or(Optional::empty);
            return organization.map(value -> RECIPIENT_ORG + value.getName()).orElse(StringUtils.EMPTY);
        }
        return StringUtils.EMPTY;
    }

    private String buildReasonCodeDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasReasonCode() && (referralRequest.getReasonCode().size() > 1)) {
            return REASON_CODE + extractReasonCodeString(referralRequest);
        }
        return StringUtils.EMPTY;
    }

    private String extractReasonCodeString(ReferralRequest referralRequest) {
        var ignoreFirstReasonCode = referralRequest.getReasonCode().subList(1, referralRequest.getReasonCode().size());
        return ignoreFirstReasonCode.stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildNoteDescription(ReferralRequest referralRequest, Bundle bundle) {
        if (referralRequest.hasNote()) {
            return referralRequest.getNote().stream()
                .map(value -> String.format(NOTE, getAuthorString(value, bundle), DateFormatUtil.formatDate(value.getTime()),  value.getText()))
                .collect(Collectors.joining(COMMA));
        }
        return StringUtils.EMPTY;
    }

    private String getAuthorString(Annotation annotation, Bundle bundle) {
        if (annotation.hasAuthorStringType()) {
            return NOTE_AUTHOR + annotation.getAuthorStringType().getValue();
        }
        else if (annotation.hasAuthorReference()) {
            String reference = annotation.getAuthorReference().getReference();
            if (reference.startsWith(ResourceType.RelatedPerson.name())) {
                var relatedPerson = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, reference).map(value -> (RelatedPerson) value).or(Optional::empty);
                return relatedPerson.map(value -> NOTE_AUTHOR_RELATION + value.getNameFirstRep().getText()).orElse(StringUtils.EMPTY);
            }
            else if (reference.startsWith(ResourceType.Practitioner.name())) {
                var practitioner = ExtractBundleResourceUtil.extractResourceFromBundle(bundle, reference).map(value -> (Practitioner) value).or(Optional::empty);
                return practitioner.map(value -> String.format(NOTE_AUTHOR_PRACTITIONER, value.getNameFirstRep().getGivenAsSingleString(), value.getNameFirstRep().getFamily())).orElse(StringUtils.EMPTY);
            }
            else if (reference.startsWith(ResourceType.Patient.name())) {
                return NOTE_AUTHOR_PATIENT;
            }
        }

        return StringUtils.EMPTY;
    }

    private String buildTextDescription(ReferralRequest referralRequest) {
        Optional<String> text = Optional.ofNullable(referralRequest.getDescription());
        return text.orElse(StringUtils.EMPTY);
    }
}
