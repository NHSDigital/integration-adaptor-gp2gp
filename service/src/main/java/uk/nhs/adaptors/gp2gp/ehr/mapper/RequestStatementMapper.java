package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractAuthor;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractNoteTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractReasonCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractRecipient;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractServiceRequested;
import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

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
    private static final String REQUESTER_DEVICE = "Requester Device: ";
    private static final String REQUESTER_PATIENT = "Requester: Patient";
    private static final String REQUESTER_ORG = "Requester Org: ";
    private static final String REQUESTER_RELATION = "Requester: Relation ";
    private static final String UBRN = "UBRN: ";
    private static final String PRIORITY = "Priority: ";
    private static final String SERVICES_REQUESTED = "Service(s) Requested: ";
    private static final String SPECIALTY = "Specialty: ";
    private static final String REASON_CODES = "Reason Codes: ";

    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String COMMA = ", ";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, boolean isNested) {
        return new InnerMapper(referralRequest, isNested).map();
    }

    @RequiredArgsConstructor
    private class InnerMapper {
        private final ReferralRequest referralRequest;
        private final boolean isNested;
        private final RequestStatementTemplateParametersBuilder templateParameters = RequestStatementTemplateParameters.builder();
        private String text = StringUtils.EMPTY;

        private String map() {
            final IdMapper idMapper = messageContext.getIdMapper();
            templateParameters
                .requestStatementId(idMapper.getOrNew(ResourceType.ReferralRequest, referralRequest.getId()))
                .isNested(isNested)
                .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTimeForReferralRequest(referralRequest))
                .code(buildCode());

            text = buildTextDescription();
            prependDescriptionInfo();

            if (!referralRequest.hasReasonCode()) {
                templateParameters.defaultReasonCode(DEFAULT_REASON_CODE_XML);
            }

            if (referralRequest.hasRequester()
                    && referralRequest.getRequester().hasAgent()
                    && referralRequest.getRequester().getAgent().hasReference()) {
                var requester = referralRequest.getRequester();
                Reference agentRef = requester.getAgent();
                var onBehalfOf = requester.hasOnBehalfOf() ? requester.getOnBehalfOf() : null;
                processAgent(agentRef, onBehalfOf);
            }

            if (referralRequest.hasRecipient()) {
                final Reference recipient = referralRequest.getRecipientFirstRep();
                final var responsibleParty = idMapper.get(recipient);
                templateParameters.responsibleParty(responsibleParty);
            }

            templateParameters.text(text);

            return TemplateUtils.fillTemplate(REQUEST_STATEMENT_TEMPLATE, templateParameters.build());
        }

        private void prependText(String operand) {
            text = operand + " " + text;
        }

        private void processAgent(Reference agent, Reference onBehalfOf) {
            final IdMapper idMapper = messageContext.getIdMapper();

            if (isReferenceToType(agent, ResourceType.Practitioner) && isReferenceToType(onBehalfOf, ResourceType.Organization)) {
                final String participantRef = idMapper.get(onBehalfOf);
                final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
                templateParameters.participant(participant);

            } else if (isReferenceToType(agent, ResourceType.Practitioner) || isReferenceToType(agent, ResourceType.Organization)) {
                final String participantRef = idMapper.getOrNew(agent);
                final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
                templateParameters.participant(participant);
            }

            if (isReferenceToType(agent, ResourceType.Device)) {
                messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .filter(Device.class::isInstance)
                    .map(Device.class::cast)
                    .filter(Device::hasType)
                    .map(Device::getType)
                    .flatMap(CodeableConceptMappingUtils::extractTextOrCoding)
                    .map(REQUESTER_DEVICE::concat)
                    .ifPresentOrElse(this::prependText, () -> {
                        throw new EhrMapperException("Could not resolve Device Reference");
                    });

            } else if (isReferenceToType(agent, ResourceType.Organization)) {
                messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .filter(Organization.class::isInstance)
                    .map(Organization.class::cast)
                    .filter(Organization::hasName)
                    .map(Organization::getName)
                    .map(REQUESTER_ORG::concat)
                    .ifPresentOrElse(this::prependText, () -> {
                        throw new EhrMapperException("Could not resolve Organization Reference");
                    });

            } else if (isReferenceToType(agent, ResourceType.Patient)) {
                messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .filter(Patient.class::isInstance)
                    .map($ -> REQUESTER_PATIENT)
                    .ifPresent(this::prependText);

            } else if (isReferenceToType(agent, ResourceType.RelatedPerson)) {
                messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .filter(RelatedPerson.class::isInstance)
                    .map(RelatedPerson.class::cast)
                    .filter(RelatedPerson::hasName)
                    .map(RelatedPerson::getNameFirstRep)
                    .map(RequestStatementExtractor::extractHumanName)
                    .map(REQUESTER_RELATION::concat)
                    .ifPresentOrElse(this::prependText, () -> {
                        throw new EhrMapperException("Could not resolve RelatedPerson Reference");
                    });

            } else if (!isReferenceToType(agent, ResourceType.Practitioner)) {
                throw new EhrMapperException("Requester Reference not of expected Resource Type");
            }
        }

        private void prependDescriptionInfo() {
            prependNoteDescription();
            prependReasonCodeDescription();
            prependRecipientDescription();
            prependSpecialtyDescription();
            prependServiceRequestedDescription();
            prependPriorityDescription();
            prependIdentifierDescription();
        }

        private void prependIdentifierDescription() {
            Optional.of(referralRequest).stream()
                .filter(ReferralRequest::hasIdentifier)
                .map(ReferralRequest::getIdentifier)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(val -> UBRN_SYSTEM_URL.equals(val.getSystem()))
                .map(Identifier::getValue)
                .findAny()
                .map(UBRN::concat)
                .filter(StringUtils::isNotBlank)
                .ifPresent(this::prependText);
        }

        private void prependPriorityDescription() {
            if (referralRequest.hasPriority()) {
                prependText(PRIORITY + referralRequest.getPriority().getDisplay());
            }
        }

        private void prependServiceRequestedDescription() {
            if (referralRequest.hasServiceRequested()) {
                prependText(SERVICES_REQUESTED + extractServiceRequested(referralRequest));
            }
        }

        private void prependSpecialtyDescription() {
            if (referralRequest.hasSpecialty()) {
                extractTextOrCoding(referralRequest.getSpecialty())
                    .map(SPECIALTY::concat)
                    .ifPresent(this::prependText);
            }
        }

        private void prependRecipientDescription() {
            if (referralRequest.hasRecipient()) {
                String recipientDescription = getRecipientsWithoutFirstPractitioner().stream()
                    .filter(Reference::hasReferenceElement)
                    .map(value -> extractRecipient(messageContext, value))
                    .collect(Collectors.joining(" "));
                prependText(recipientDescription);
            }
        }

        private List<Reference> getRecipientsWithoutFirstPractitioner() {
            boolean firstPractitionerFound = false;
            var recipients = new ArrayList<Reference>(referralRequest.getRecipient().size());
            for (Reference reference : referralRequest.getRecipient()) {
                if (!firstPractitionerFound && isReferenceToPractitioner(reference)) {
                    firstPractitionerFound = true;
                } else {
                    recipients.add(reference);
                }
            }
            return recipients;
        }

        private void prependReasonCodeDescription() {
            if (referralRequest.hasReasonCode() && referralRequest.getReasonCode().size() > 1) {
                prependText(REASON_CODES + extractReasonCode(referralRequest));
            }
        }

        private void prependNoteDescription() {
            if (referralRequest.hasNote()) {
                String noteDescription = referralRequest.getNote().stream()
                    .map(value -> String.format(NOTE, extractAuthor(messageContext, value), extractNoteTime(value), value.getText()))
                    .collect(Collectors.joining(COMMA));
                prependText(noteDescription);
            }
        }

        private String buildTextDescription() {
            if (referralRequest.hasDescription()) {
                return referralRequest.getDescription();
            }
            return StringUtils.EMPTY;
        }

        private String buildCode() {
            if (referralRequest.hasReasonCode()) {
                return codeableConceptCdMapper.mapCodeableConceptToCd(referralRequest.getReasonCodeFirstRep());
            }
            return StringUtils.EMPTY;
        }
    }

    private static boolean isReferenceToPractitioner(Reference reference) {
        return isReferenceToType(reference, ResourceType.Practitioner);
    }

    private static boolean isReferenceToType(@NonNull Reference reference, @NonNull ResourceType type) {
        return reference.getReference().startsWith(type.name() + "/");
    }
}
