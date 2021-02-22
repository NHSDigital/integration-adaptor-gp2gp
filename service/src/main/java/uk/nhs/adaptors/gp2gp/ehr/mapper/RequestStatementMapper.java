package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractAuthor;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractDevice;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractHumanName;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractNoteTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractReasonCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractRecipient;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractServiceRequested;
import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;

import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
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
    private static final String REASON_CODE = "Reason Codes: ";
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
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
        }
        return StringUtils.EMPTY;
    }

    private String buildServiceRequestedDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasServiceRequested()) {
            return SERVICE_REQUESTED + extractServiceRequested(referralRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildRequesterDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasRequester() && referralRequest.getRequester().getAgent().hasReference()) {
            IIdType reference = referralRequest.getRequester().getAgent().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Device.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Device) resource)
                    .map(value -> extractDevice(value))
                    .map(text -> REQUESTER_DEVICE + text)
                    .orElseThrow(() -> new EhrMapperException("Could not resolve Device Reference"));
            } else if (reference.getResourceType().equals(ResourceType.Organization.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Organization) resource)
                    .map(value -> REQUESTER_ORG + value.getName())
                    .orElseThrow(() -> new EhrMapperException("Could not resolve Organization Reference"));
            } else if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return REQUESTER_PATIENT;
            } else if (reference.getResourceType().equals(ResourceType.RelatedPerson.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (RelatedPerson) resource)
                    .map(value -> REQUESTER_RELATION + extractHumanName(value.getNameFirstRep()))
                    .orElseThrow(() -> new EhrMapperException("Could not resolve RelatedPerson Reference"));
            } else if (reference.getResourceType().equals(ResourceType.Practitioner.name())) {
                return StringUtils.EMPTY;
            }
            throw new EhrMapperException("Requester Reference not of expected Resource Type");
        }

        return StringUtils.EMPTY;
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
            return getRecipientsWithoutFirstPractitioner(referralRequest).stream()
                .filter(Reference::hasReferenceElement)
                .map(value -> extractRecipient(messageContext, value))
                .collect(Collectors.joining(COMMA));
        }

        return StringUtils.EMPTY;
    }

    private List<Reference> getRecipientsWithoutFirstPractitioner(ReferralRequest referralRequest) {
        boolean firstPractitionerFound = false;
        List<Reference> recipients = new ArrayList<>(referralRequest.getRecipient().size());
        for (Reference reference : referralRequest.getRecipient()) {
            if (!firstPractitionerFound && isReferenceToPractitioner(reference)) {
                firstPractitionerFound = true;
            } else {
                recipients.add(reference);
            }
        }
        return recipients;
    }

    private boolean isReferenceToPractitioner(Reference reference) {
        return reference.getReference().startsWith(ResourceType.Practitioner.name());
    }

    private String buildReasonCodeDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasReasonCode() && (referralRequest.getReasonCode().size() > 1)) {
            return REASON_CODE + extractReasonCode(referralRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildNoteDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasNote()) {
            return referralRequest.getNote().stream()
                .map(value -> String.format(NOTE, extractAuthor(messageContext, value), extractNoteTime(value),  value.getText()))
                .collect(Collectors.joining(COMMA));
        }
        return StringUtils.EMPTY;
    }

    private String buildTextDescription(ReferralRequest referralRequest) {
        Optional<String> text = Optional.ofNullable(referralRequest.getDescription());
        return text.orElse(StringUtils.EMPTY);
    }
}
