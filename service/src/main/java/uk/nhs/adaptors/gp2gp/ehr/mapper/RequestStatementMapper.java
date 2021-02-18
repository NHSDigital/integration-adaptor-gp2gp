package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;

import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
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
    private static final String RECIPIENT_PRACTITIONER = "Recipient Practitioner: ";
    private static final String RECIPIENT_HEALTH_CARE_SERVICE = "Recipient Healthcare Service: ";
    private static final String RECIPIENT_ORG = "Recipient Org: ";
    private static final String REASON_CODE = "Reason Codes: ";
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String NOTE_AUTHOR = "Author: ";
    private static final String NOTE_AUTHOR_RELATION = NOTE_AUTHOR + "Relation ";
    private static final String NOTE_AUTHOR_PRACTITIONER = NOTE_AUTHOR + "Practitioner ";
    private static final String NOTE_AUTHOR_PATIENT = NOTE_AUTHOR + "Patient";
    private static final String COMMA = ",";
    private static final String NAME = "%s %s";

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
            .map(value -> extractTextOrCoding(value))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildRequesterDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasRequester() && referralRequest.getRequester().getAgent().hasReference()) {
            IIdType reference = referralRequest.getRequester().getAgent().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Device.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Device) resource)
                    .map(this::getDeviceCode)
                    .map(text -> REQUESTER_DEVICE + text)
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Organization.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Organization) resource)
                    .map(value -> REQUESTER_ORG + value.getName())
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return REQUESTER_PATIENT;
            } else if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (RelatedPerson) resource)
                    .map(value -> REQUESTER_RELATION + getHumanNameString(value.getNameFirstRep()))
                    .orElse(StringUtils.EMPTY);
            }
        }

        return StringUtils.EMPTY;
    }

    private String getDeviceCode(Device device) {
        return extractTextOrCoding(device.getType()).orElse(StringUtils.EMPTY);
    }

    private String buildSpecialtyDescription(ReferralRequest referralRequest) {
        Optional<String> specialty = Optional.empty();
        if (referralRequest.hasSpecialty()) {
            CodeableConcept specialtyObject = referralRequest.getSpecialty();
            specialty = extractTextOrCoding(specialtyObject);
        }
        return specialty.map(value -> SPECIALTY + value).orElse(StringUtils.EMPTY);
    }

    private String buildRecipientDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasRecipient()) {
            var ignoreFirstPractitioner = removeFirstPractitionerReference(referralRequest.getRecipient());

            return ignoreFirstPractitioner.stream()
                .filter(Reference::hasReferenceElement)
                .map(value -> extractRecipientString(value))
                .collect(Collectors.joining(COMMA));
        }

        return StringUtils.EMPTY;
    }

    private List<Reference> removeFirstPractitionerReference(List<Reference> referenceList) {
        for (int i = 0; i < referenceList.size() - 1; i++) {
            if (referenceList.get(i).getReference().startsWith(ResourceType.Practitioner.name())) {
                referenceList.remove(i);
                return referenceList;
            }
        }
        return referenceList;
    }

    private String extractRecipientString(Reference reference) {
        IIdType referenceId = reference.getReferenceElement();
        if (referenceId.getResourceType().equals(ResourceType.Practitioner.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (Practitioner) resource)
                .map(value -> RECIPIENT_PRACTITIONER + getHumanNameString(value.getNameFirstRep()))
                .orElse(StringUtils.EMPTY);
        } else if (referenceId.getResourceType().equals(ResourceType.HealthcareService.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (HealthcareService) resource)
                .map(value -> RECIPIENT_HEALTH_CARE_SERVICE + value.getName())
                .orElse(StringUtils.EMPTY);
        } else if (referenceId.getResourceType().equals(ResourceType.Organization.name())) {
            return messageContext.getInputBundleHolder()
                .getResource(referenceId)
                .map(resource -> (Organization) resource)
                .map(value -> RECIPIENT_ORG + value.getName())
                .orElse(StringUtils.EMPTY);
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
            .map(value -> extractTextOrCoding(value))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(COMMA));
    }

    private String buildNoteDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasNote()) {
            return referralRequest.getNote().stream()
                .map(value -> String.format(NOTE, getAuthorString(value), getNoteTime(value),  value.getText()))
                .collect(Collectors.joining(COMMA));
        }
        return StringUtils.EMPTY;
    }

    private String getAuthorString(Annotation annotation) {
        if (annotation.hasAuthorStringType() && annotation.getAuthorStringType().hasValue()) {
            return NOTE_AUTHOR + annotation.getAuthorStringType().getValue();
        } else if (annotation.hasAuthorReference() && annotation.getAuthorReference().hasReferenceElement()) {
            IIdType reference = annotation.getAuthorReference().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (RelatedPerson) resource)
                    .map(value -> NOTE_AUTHOR_RELATION + getHumanNameString(value.getNameFirstRep()))
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Practitioner.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Practitioner) resource)
                    .map(value -> NOTE_AUTHOR_PRACTITIONER + getHumanNameString(value.getNameFirstRep()))
                    .orElse(StringUtils.EMPTY);
            } else if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return NOTE_AUTHOR_PATIENT;
            }
        }

        return StringUtils.EMPTY;
    }

    private String getHumanNameString(HumanName humanName) {
        if  (humanName.hasText()) {
            return humanName.getText();
        } else if (humanName.hasGiven() || humanName.hasFamily()) {
            String given = Optional.ofNullable(humanName.getGivenAsSingleString()).orElse(StringUtils.EMPTY);
            String family = Optional.ofNullable(humanName.getFamily()).orElse(StringUtils.EMPTY);
            return String.format(NAME, given, family);
        }
        return StringUtils.EMPTY;
    }

    private String getNoteTime(Annotation annotation) {
        if (annotation.hasTime()) {
            return DateFormatUtil.formatDate(annotation.getTime());
        }
        return StringUtils.EMPTY;
    }

    private String buildTextDescription(ReferralRequest referralRequest) {
        Optional<String> text = Optional.ofNullable(referralRequest.getDescription());
        return text.orElse(StringUtils.EMPTY);
    }
}
