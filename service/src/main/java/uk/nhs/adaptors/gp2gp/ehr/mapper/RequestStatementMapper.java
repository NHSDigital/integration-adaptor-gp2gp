package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractAuthor;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractNoteTime;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractReasonCode;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractRecipient;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractServiceRequested;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementExtractor.extractSupportingInfo;
import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
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
    private static final String SERVICES_REQUESTED = "Service(s) Requested: ";
    private static final String SPECIALTY = "Specialty: ";
    private static final String REASON_CODES = "Reason Codes: ";
    private static final String SUPPORTING_INFO = "Supporting Information: ";
    private static final String PRIORITY_ASAP = "Priority: ASAP";
    private static final Set<String> SUPPORTED_AGENT_TYPES = Stream.of(ResourceType.Practitioner, ResourceType.RelatedPerson,
        ResourceType.Device, ResourceType.Patient, ResourceType.Organization)
        .map(ResourceType::name)
        .collect(Collectors.toSet());
    private static final String DEFAULT_REASON_CODE_XML = "<code code=\"3457005\" displayName=\"Patient referral\" codeSystem=\"2.16.840.1"
        + ".113883.2.1.3.2.4.15\"/>";
    private static final String NOTE = "Annotation: %s @ %s %s";
    private static final String NOTE_WITH_NO_AUTHOR_AND_TIME = "Annotation: %s";
    private static final String COMMA = ", ";

    private static final String PRIORITY_CODE_IMMEDIATE =
        "<priorityCode code=\"88694003\" displayName=\"Immediate\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\">"
            + "<originalText>Asap</originalText>"
            + "</priorityCode>";

    private static final String PRIORITY_CODE_NORMAL =
        "<priorityCode code=\"394848005\" displayName=\"Normal\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\">"
            + "<originalText>Routine</originalText>"
            + "</priorityCode>";

    private static final String PRIORITY_CODE_HIGH =
        "<priorityCode code=\"394849002\" displayName=\"High\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.15\">"
            + "<originalText>Urgent</originalText>"
            + "</priorityCode>";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private final ParticipantMapper participantMapper;

    public String mapReferralRequestToRequestStatement(ReferralRequest referralRequest, boolean isNested) {
        return new InnerMapper(referralRequest, isNested).map();
    }

    @RequiredArgsConstructor
    private final class InnerMapper {
        private final ReferralRequest referralRequest;
        private final boolean isNested;
        private final RequestStatementTemplateParametersBuilder templateParameters = RequestStatementTemplateParameters.builder();

        private String map() {
            if (hasReferencingAgent()) {
                var requester = referralRequest.getRequester();
                Reference agentRef = requester.getAgent();
                if (!SUPPORTED_AGENT_TYPES.contains(agentRef.getReference().split("/")[0])) {
                    throw new EhrMapperException("Requester Reference not of expected Resource Type");
                }
                var onBehalfOf = requester.hasOnBehalfOf() ? requester.getOnBehalfOf() : null;
                processAgent(agentRef, onBehalfOf);
            }

            final IdMapper idMapper = messageContext.getIdMapper();
            templateParameters
                .requestStatementId(idMapper.getOrNew(ResourceType.ReferralRequest, referralRequest.getIdElement()))
                .isNested(isNested)
                .availabilityTime(StatementTimeMappingUtils.prepareAvailabilityTime(referralRequest.getAuthoredOnElement()))
                .text(buildTextDescription())
                .priorityCode(buildPriorityCode())
                .code(buildCode());

            if (referralRequest.hasRecipient()) {
                referralRequest.getRecipient().stream()
                    .filter(RequestStatementMapper::isReferenceToPractitioner)
                    .findAny()
                    .map(messageContext.getAgentDirectory()::getAgentId)
                    .ifPresent(templateParameters::responsibleParty);
            }

            return TemplateUtils.fillTemplate(REQUEST_STATEMENT_TEMPLATE, templateParameters.build());
        }

        private String buildPriorityCode() {
            if (!referralRequest.hasPriority()) {
                return null;
            }

            switch (referralRequest.getPriority()) {
                case ASAP:
                    return null;
                case ROUTINE:
                    return PRIORITY_CODE_NORMAL;
                case URGENT:
                    return PRIORITY_CODE_HIGH;
                default:
                    throw new EhrMapperException(
                        String.format("Unsupported priority in ReferralRequest: %s", referralRequest.getPriority().toCode())
                    );
            }
        }

        private boolean hasReferencingAgent() {
            return referralRequest.hasRequester()
                && referralRequest.getRequester().hasAgent()
                && referralRequest.getRequester().getAgent().hasReference();
        }

        private void processAgent(@NonNull Reference agent, Reference onBehalfOf) {
            AgentDirectory agentDirectory = messageContext.getAgentDirectory();

            if (isReferenceToPractitioner(agent)
                && onBehalfOf != null && isReferenceToType(onBehalfOf, ResourceType.Organization)) {
                final String participantRef = agentDirectory.getAgentRef(agent, onBehalfOf);
                final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
                templateParameters.participant(participant);

            } else if (isReferenceToPractitioner(agent)) {
                final String participantRef = agentDirectory.getAgentId(agent);
                final String participant = participantMapper.mapToParticipant(participantRef, ParticipantType.AUTHOR);
                templateParameters.participant(participant);
            }
        }

        private String buildIdentifierDescription() {
            return Optional.of(referralRequest).stream()
                .filter(ReferralRequest::hasIdentifier)
                .map(ReferralRequest::getIdentifier)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(val -> UBRN_SYSTEM_URL.equals(val.getSystem()))
                .map(Identifier::getValue)
                .findAny()
                .map(UBRN::concat)
                .filter(StringUtils::isNotBlank)
                .orElse(StringUtils.EMPTY);
        }

        private String buildServiceRequestedDescription() {
            if (referralRequest.hasServiceRequested()) {
                return SERVICES_REQUESTED + extractServiceRequested(referralRequest);
            }
            return StringUtils.EMPTY;
        }

        private String buildSpecialtyDescription() {
            if (referralRequest.hasSpecialty()) {
                return extractTextOrCoding(referralRequest.getSpecialty())
                    .map(SPECIALTY::concat)
                    .orElse(StringUtils.EMPTY);
            }
            return StringUtils.EMPTY;
        }

        private String buildRecipientDescription() {
            if (referralRequest.hasRecipient()) {
                return getRecipientsWithoutFirstPractitioner().stream()
                    .filter(Reference::hasReferenceElement)
                    .map(value -> extractRecipient(messageContext, value))
                    .collect(Collectors.joining(" "));
            }
            return StringUtils.EMPTY;
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

        private String buildReasonCodeDescription() {
            if (referralRequest.hasReasonCode() && referralRequest.getReasonCode().size() > 1) {
                return REASON_CODES + extractReasonCode(referralRequest);
            }
            return StringUtils.EMPTY;
        }

        private String buildNoteDescription() {
            if (referralRequest.hasNote()) {
                return referralRequest.getNote().stream()
                    .map(this::buildConditionalNoteDesc)
                    .collect(Collectors.joining(COMMA));
            }
            return StringUtils.EMPTY;
        }

        private String buildDeviceDescription(Reference agent) {
            if (isReferenceToType(agent, ResourceType.Device)) {
                return messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .map(Device.class::cast)
                    .filter(Device::hasType)
                    .map(Device::getType)
                    .flatMap(CodeableConceptMappingUtils::extractTextOrCoding)
                    .map(REQUESTER_DEVICE::concat)
                    .get();
            }
            return StringUtils.EMPTY;
        }

        private String buildOrganizationDescription(Reference agent) {
            if (isReferenceToType(agent, ResourceType.Organization)) {
                return messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .map(Organization.class::cast)
                    .filter(Organization::hasName)
                    .map(Organization::getName)
                    .map(REQUESTER_ORG::concat)
                    .get();
            }
            return StringUtils.EMPTY;
        }

        private String buildPatientDescription(Reference agent) {
            if (isReferenceToType(agent, ResourceType.Patient)) {
                return messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .map($ -> REQUESTER_PATIENT)
                    .orElse(StringUtils.EMPTY);
            }
            return StringUtils.EMPTY;
        }

        private String buildRelatedPersonDescription(Reference agent) {
            if (isReferenceToType(agent, ResourceType.RelatedPerson)) {
                return messageContext.getInputBundleHolder().getResource(agent.getReferenceElement())
                    .map(RelatedPerson.class::cast)
                    .filter(RelatedPerson::hasName)
                    .map(RelatedPerson::getNameFirstRep)
                    .map(RequestStatementExtractor::extractHumanName)
                    .map(REQUESTER_RELATION::concat)
                    .get();
            }
            return StringUtils.EMPTY;
        }

        private String buildTextDescription() {
            StringBuilder text = new StringBuilder();
            if (referralRequest.hasDescription()) {
                text.append(referralRequest.getDescription());
            }
            Stream<String> descriptions = Stream.of(
                buildNoteDescription(),
                buildReasonCodeDescription(),
                buildRecipientDescription(),
                buildSpecialtyDescription(),
                buildServiceRequestedDescription(),
                buildIdentifierDescription(),
                buildSupportingInfoDescription(),
                buildPriorityDescription()
            );

            if (hasReferencingAgent()) {
                Reference agent = referralRequest.getRequester().getAgent();
                descriptions = Stream.concat(descriptions, Stream.of(
                    buildDeviceDescription(agent),
                    buildOrganizationDescription(agent),
                    buildPatientDescription(agent),
                    buildRelatedPersonDescription(agent)
                ));
            }

            descriptions
                .filter(StringUtils::isNotBlank)
                .map(description -> description + " ")
                .forEach(description -> text.insert(0, description));

            return text.toString();
        }

        private String buildCode() {
            if (referralRequest.hasReasonCode()) {
                return codeableConceptCdMapper.mapCodeableConceptToCd(referralRequest.getReasonCodeFirstRep());
            }
            return DEFAULT_REASON_CODE_XML;
        }

        private String buildSupportingInfoDescription() {
            if (referralRequest.hasSupportingInfo()) {
                String supportingInfo = SUPPORTING_INFO + referralRequest.getSupportingInfo().stream()
                    .map(value -> extractSupportingInfo(messageContext, value))
                    .filter(value -> !value.equals(StringUtils.EMPTY))
                    .collect(Collectors.joining(COMMA));

                return supportingInfo.equals(SUPPORTING_INFO) ? StringUtils.EMPTY : supportingInfo;
            }
            return StringUtils.EMPTY;
        }

        private String buildPriorityDescription() {
            if (referralRequest.hasPriority() && referralRequest.getPriority() == ReferralRequest.ReferralPriority.ASAP) {
                return PRIORITY_ASAP;
            }

            return StringUtils.EMPTY;
        }

        private String buildConditionalNoteDesc(Annotation value) {
            String author = extractAuthor(messageContext, value);
            String time = extractNoteTime(value);

            return author.isEmpty() && time.isEmpty()
                ? String.format(NOTE_WITH_NO_AUTHOR_AND_TIME, value.getText())
                : String.format(NOTE, author, time, value.getText());
        }
    }

    private static boolean isReferenceToPractitioner(Reference reference) {
        return isReferenceToType(reference, ResourceType.Practitioner);
    }

    private static boolean isReferenceToType(@NonNull Reference reference, @NonNull ResourceType type) {
        return reference.getReference().startsWith(type.name() + "/");
    }
}
