package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractAuthor;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractNoteTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractReasonCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractRecipient;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractServiceRequested;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.RequestStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.RequestStatementTemplateParameters.RequestStatementTemplateParametersBuilder;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RequestStatementMapper {
    private static final Mustache REQUEST_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_request_statement_template.mustache");

    private static final String UBRN_SYSTEM_URL = "https://fhir.nhs.uk/Id/ubr-number";
    private static final String REQUESTER_PATIENT = "Requester: Patient";
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String COMMA = ", ";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();
        var requestStatementTemplateParameters = RequestStatementTemplateParameters.builder()
            .requestStatementId(idMapper.getOrNew(ResourceType.ReferralRequest, referralRequest.getId()))
            .isNested(isNested)
            .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForReferralRequest(referralRequest))
            .text(buildDescription(referralRequest))
            .code(buildCode(referralRequest));

        if (!referralRequest.hasReasonCode()) {
            requestStatementTemplateParameters.defaultReasonCode(DEFAULT_REASON_CODE_XML);
        }

        if (referralRequest.hasRequester()
                && referralRequest.getRequester().hasAgent()
                && referralRequest.getRequester().getAgent().hasReference()) {
            var requester = referralRequest.getRequester();
            Reference agentRef = requester.getAgent();
            var onBehalfOf = requester.hasOnBehalfOf() ? requester.getOnBehalfOf() : null;
            processAgent(agentRef, onBehalfOf, requestStatementTemplateParameters);
        }

        if (referralRequest.hasRecipient()) {
            final Reference recipient = referralRequest.getRecipientFirstRep();
            final var responsibleParty = idMapper.get(recipient);
            requestStatementTemplateParameters.responsibleParty(responsibleParty);
        }

        return TemplateUtils.fillTemplate(REQUEST_STATEMENT_TEMPLATE, requestStatementTemplateParameters.build());
    }

    private void processAgent(Reference agent, Reference onBehalfOf,
                              RequestStatementTemplateParametersBuilder requestStatementTemplateParameters) {
        final IdMapper idMapper = messageContext.getIdMapper();

        if (isReferenceToType(agent, ResourceType.Practitioner) && isReferenceToType(onBehalfOf, ResourceType.Organization)) {
            final String participantRef = idMapper.get(onBehalfOf);
            final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
            requestStatementTemplateParameters.participant(participant);

        } else if (isReferenceToType(agent, ResourceType.Practitioner) || isReferenceToType(agent, ResourceType.Organization)) {
            final String participantRef = idMapper.getOrNew(agent);
            final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
            requestStatementTemplateParameters.participant(participant);
        }

        if (isReferenceToType(agent, ResourceType.Device)) {
            messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                .filter(Device.class::isInstance)
                .map(Device.class::cast)
                .filter(Device::hasType)
                .map(Device::getType)
                .flatMap(CodeableConceptMappingUtils::extractTextOrCoding)
                .map("Requester Device: "::concat)
                .ifPresentOrElse(requestStatementTemplateParameters::text, () -> {
                    throw new EhrMapperException("Could not resolve Device Reference");
                });

        } else if (isReferenceToType(agent, ResourceType.Organization)) {
            messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                .filter(Organization.class::isInstance)
                .map(Organization.class::cast)
                .filter(Organization::hasName)
                .map(Organization::getName)
                .map("Requester Org: "::concat)
                .ifPresent(requestStatementTemplateParameters::text);

        } else if (isReferenceToType(agent, ResourceType.Patient)) {
            messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                .filter(Patient.class::isInstance)
                .map($ -> REQUESTER_PATIENT)
                .ifPresent(requestStatementTemplateParameters::text);

        } else if (isReferenceToType(agent, ResourceType.RelatedPerson)) {
            messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                .filter(RelatedPerson.class::isInstance)
                .map(RelatedPerson.class::cast)
                .filter(RelatedPerson::hasName)
                .map(RelatedPerson::getNameFirstRep)
                .map(RequestStatementExtractor::extractHumanName)
                .map("Requester: Relation "::concat)
                .ifPresent(requestStatementTemplateParameters::text);

        } else if (!isReferenceToType(agent, ResourceType.Practitioner)) {
            throw new EhrMapperException("Requester Reference not of expected Resource Type");
        }
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
            buildSpecialtyDescription(referralRequest),
            buildRecipientDescription(referralRequest),
            buildReasonCodeDescription(referralRequest),
            buildNoteDescription(referralRequest),
            buildTextDescription(referralRequest)
        );
    }

    private String buildIdentifierDescription(ReferralRequest referralRequest) {
        return Optional.of(referralRequest).stream()
            .filter(ReferralRequest::hasIdentifier)
            .map(ReferralRequest::getIdentifier)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .filter(val -> UBRN_SYSTEM_URL.equals(val.getSystem()))
            .map(Identifier::getValue)
            .findAny()
            .map("UBRN: "::concat)
            .orElse(StringUtils.EMPTY);
    }

    private String buildPriorityDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasPriority()) {
            return "Priority: " + referralRequest.getPriority().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildServiceRequestedDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasServiceRequested()) {
            return "Service(s) Requested: " + extractServiceRequested(referralRequest);
        }
        return StringUtils.EMPTY;
    }

    private String buildSpecialtyDescription(ReferralRequest referralRequest) {
        return Optional.of(referralRequest)
            .filter(ReferralRequest::hasSpecialty)
            .map(ReferralRequest::getSpecialty)
            .flatMap(CodeableConceptMappingUtils::extractTextOrCoding)
            .map("Specialty: "::concat)
            .orElse(StringUtils.EMPTY);
    }

    private String buildRecipientDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasRecipient()) {
            return getRecipientsWithoutFirstPractitioner(referralRequest).stream()
                .filter(Reference::hasReferenceElement)
                .map(value -> extractRecipient(messageContext, value))
                .collect(Collectors.joining(" "));
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
        return isReferenceToType(reference, ResourceType.Practitioner);
    }

    private boolean isReferenceToType(@NonNull Reference reference, @NonNull ResourceType type) {
        return reference.getReference().startsWith(type.name() + "/");
    }

    private String buildReasonCodeDescription(ReferralRequest referralRequest) {
        if (referralRequest.hasReasonCode() && referralRequest.getReasonCode().size() > 1) {
            return "Reason Codes: " + extractReasonCode(referralRequest);
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

    private String buildCode(ReferralRequest referralRequest) {
        if (referralRequest.hasReasonCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(referralRequest.getReasonCodeFirstRep());
        }
        return StringUtils.EMPTY;
    }
}
