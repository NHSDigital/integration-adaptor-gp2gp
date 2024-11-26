package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType.LABORATORY_RESULT_COMMENT;
import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper.NARRATIVE_STATEMENT_TEMPLATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.ConfidentialityService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CommentType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.DiagnosticReportCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.NarrativeStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class DiagnosticReportMapper {

    public static final String DUMMY_SPECIMEN_ID_PREFIX = "DUMMY-SPECIMEN-";
    public static final String DUMMY_OBSERVATION_ID_PREFIX = "DUMMY-OBSERVATION-";

    private static final Mustache DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("diagnostic_report_compound_statement_template.mustache");

    private static final String PREPENDED_TEXT_FOR_CONCLUSION_COMMENT = "Interpretation: ";
    private static final String PREPENDED_TEXT_FOR_CODED_DIAGNOSIS = "Lab Diagnosis: ";
    private static final String PREPENDED_TEXT_FOR_STATUS = "Status: ";
    private static final String PREPENDED_TEXT_FOR_FILLING_DATE = "Filing Date: ";
    private static final String PREPENDED_TEXT_FOR_PARTICIPANTS = "Participants: ";

    private static final String PMIP_CODE_SYSTEM = "2.16.840.1.113883.2.1.4.5.5";
    private static final String COMMENT_NOTE = "37331000000100";
    public static final String URN_OID_PREFIX = "urn:oid:";

    private final MessageContext messageContext;
    private final SpecimenMapper specimenMapper;
    private final ParticipantMapper participantMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final ConfidentialityService confidentialityService;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport) {
        List<Observation> observations = fetchObservations(diagnosticReport);
        List<Specimen> specimens = fetchSpecimens(diagnosticReport, observations);
        final IdMapper idMapper = messageContext.getIdMapper();
        markObservationsAsProcessed(idMapper, observations);

        List<Observation> observationsExcludingFilingComments = assignDummySpecimensToObservationsWithNoSpecimen(
                observations.stream()
                    .filter(Predicate.not(DiagnosticReportMapper::isFilingComment))
                    .toList(),
                specimens);

        String mappedSpecimens = specimens.stream()
            .map(specimen -> specimenMapper.mapSpecimenToCompoundStatement(specimen,
                    observationsForSpecimen(specimen, observationsExcludingFilingComments),
                    diagnosticReport))
            .collect(Collectors.joining());

        String reportLevelNarrativeStatements = prepareReportLevelNarrativeStatements(diagnosticReport, observations);

        var diagnosticReportCompoundStatementTemplateParameters = DiagnosticReportCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.DiagnosticReport, diagnosticReport.getIdElement()))
            .extensionId(fetchExtensionId(diagnosticReport.getIdentifier()))
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTime(diagnosticReport.getIssuedElement()))
            .narrativeStatements(reportLevelNarrativeStatements)
            .specimens(mappedSpecimens)
            .confidentialityCode(confidentialityService.generateConfidentialityCode(diagnosticReport).orElse(null));

        if (diagnosticReport.hasPerformer() && diagnosticReport.getPerformerFirstRep().hasActor()) {
            final String participantReference = messageContext.getAgentDirectory().getAgentId(
                diagnosticReport.getPerformerFirstRep().getActor());
            final String participantBlock = participantMapper.mapToParticipant(participantReference, ParticipantType.AUTHOR);
            diagnosticReportCompoundStatementTemplateParameters.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(
            DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE,
            diagnosticReportCompoundStatementTemplateParameters.build()
        );
    }

    private List<Observation> observationsForSpecimen(Specimen specimen, List<Observation> observations) {
        return observations.stream()
                .filter(Observation::hasSpecimen)
                .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
                .collect(Collectors.toList());
    }

    private String fetchExtensionId(List<Identifier> identifiers) {
        return identifiers.stream()
            .filter(DiagnosticReportMapper::isPMIPCodeSystem)
            .map(Identifier::getValue)
            .findFirst()
            .orElse(StringUtils.EMPTY);
    }

    private List<Specimen> fetchSpecimens(DiagnosticReport diagnosticReport, List<Observation> observations) {

        List<Specimen> specimens = new ArrayList<>();

        // At least one specimen is required to exist for any DiagnosticReport, according to the mim
        if (!diagnosticReport.hasSpecimen() || hasObservationsWithoutSpecimen(observations)) {
            specimens.add(generateDummySpecimen(diagnosticReport));
        }

        var inputBundleHolder = messageContext.getInputBundleHolder();
        List<Specimen> nonDummySpecimens = diagnosticReport.getSpecimen()
            .stream()
            .map(specimenReference -> inputBundleHolder.getResource(specimenReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Specimen.class::cast)
            .collect(Collectors.toList());

        specimens.addAll(nonDummySpecimens);

        return specimens;

    }

    private boolean hasObservationsWithoutSpecimen(List<Observation> observations) {
        return observations
                .stream()
                .filter(observation -> !isFilingComment(observation))
                .anyMatch(observation -> !observation.hasSpecimen());
    }

    private List<Observation> assignDummySpecimensToObservationsWithNoSpecimen(
            List<Observation> observations, List<Specimen> specimens) {

        if (!hasObservationsWithoutSpecimen(observations)) {
            return observations;
        }

        // The assumption was made that all test results without a specimen will have the same dummy specimen referenced
        Specimen dummySpecimen = specimens.stream()
                .filter(specimen -> specimen.getId().contains(DUMMY_SPECIMEN_ID_PREFIX))
                .toList().getFirst();

        Reference dummySpecimenReference = new Reference(dummySpecimen.getId());

        for (Observation observation : observations) {
            if (!observation.hasSpecimen() && !isFilingComment(observation)) {
                observation.setSpecimen(dummySpecimenReference);
            }
        }

        return observations;
    }

    private Specimen generateDummySpecimen(DiagnosticReport diagnosticReport) {
        Specimen specimen = new Specimen();

        specimen.setId(DUMMY_SPECIMEN_ID_PREFIX + randomIdGeneratorService.createNewId());

        return specimen
            .setAccessionIdentifier(new Identifier().setValue("DUMMY"))
            .setCollection(new Specimen.SpecimenCollectionComponent().setCollected(new DateTimeType(diagnosticReport.getIssued())))
            .setType(new CodeableConcept().setText("UNKNOWN"));
    }

    private List<Observation> fetchObservations(DiagnosticReport diagnosticReport) {
        if (!diagnosticReport.hasResult()) {
            return Collections.singletonList(generateDefaultObservation(diagnosticReport));
        }

        var inputBundleHolder = messageContext.getInputBundleHolder();
        return diagnosticReport.getResult().stream()
            .map(Reference::getReferenceElement)
            .map(inputBundleHolder::getResource)
            .flatMap(Optional::stream)
            .map(Observation.class::cast)
            .collect(Collectors.toList());
    }

    private Observation generateDefaultObservation(DiagnosticReport diagnosticReport) {
        Observation observation = new Observation();

        observation.setId(DUMMY_OBSERVATION_ID_PREFIX + randomIdGeneratorService.createNewId());

        return observation
            .setIssuedElement(diagnosticReport.getIssuedElement())
            .setComment("EMPTY REPORT");
    }

    private String prepareReportLevelNarrativeStatements(DiagnosticReport diagnosticReport, List<Observation> observations) {
        StringBuilder reportLevelNarrativeStatements = new StringBuilder();

        if (diagnosticReport.hasConclusion()) {
            String comment = PREPENDED_TEXT_FOR_CONCLUSION_COMMENT + diagnosticReport.getConclusion();

            String narrativeStatementFromConclusion = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport.getIssuedElement(), LABORATORY_RESULT_COMMENT.getCode(), comment, null
            );

            reportLevelNarrativeStatements.append(narrativeStatementFromConclusion);
        }

        String codedDiagnosisText = diagnosticReport.getCodedDiagnosis().stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .flatMap(Optional::stream)
            .collect(Collectors.joining(", "));

        if (!codedDiagnosisText.isEmpty()) {
            String comment = PREPENDED_TEXT_FOR_CODED_DIAGNOSIS + codedDiagnosisText;

            String narrativeStatementFromCodedDiagnosis = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport.getIssuedElement(), LABORATORY_RESULT_COMMENT.getCode(), comment, null
            );

            reportLevelNarrativeStatements.append(narrativeStatementFromCodedDiagnosis);
        }

        if (diagnosticReport.hasStatus()) {
            String status = PREPENDED_TEXT_FOR_STATUS + diagnosticReport.getStatus().toCode();
            String statusNarrativeStatement = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport.getIssuedElement(), LABORATORY_RESULT_COMMENT.getCode(), status, null);

            reportLevelNarrativeStatements.append(statusNarrativeStatement);
        }

        buildNarrativeStatementForMissingResults(diagnosticReport, reportLevelNarrativeStatements);
        buildNarrativeStatementForObservationTimes(observations, reportLevelNarrativeStatements, diagnosticReport.getIssuedElement());
        buildNarrativeStatementForParticipants(diagnosticReport, reportLevelNarrativeStatements);
        buildNarrativeStatementForObservationComments(diagnosticReport.getIssuedElement(), observations, reportLevelNarrativeStatements);

        return reportLevelNarrativeStatements.toString();
    }

    private void buildNarrativeStatementForObservationTimes(
        List<Observation> observations,
        StringBuilder reportLevelNarrativeStatements,
        InstantType diagnosticReportIssued) {
        observations.stream()
            .filter(observation -> observation.hasEffectiveDateTimeType() || observation.hasEffectivePeriod())
            .filter(DiagnosticReportMapper::isFilingComment)
            .findFirst()
            .map(observation -> buildNarrativeStatementForDiagnosticReport(
                diagnosticReportIssued,
                CommentType.AGGREGATE_COMMENT_SET.getCode(),
                extractDateFromObservation(observation),
                confidentialityService.generateConfidentialityCode(observation).orElse(null)
            ))
            .ifPresent(reportLevelNarrativeStatements::append);
    }

    private String extractDateFromObservation(Observation observation) {
        if (observation.hasEffectiveDateTimeType()) {
            return PREPENDED_TEXT_FOR_FILLING_DATE
                + DateFormatUtil.toTextFormat(observation.getEffectiveDateTimeType());
        }
        if (observation.hasEffectivePeriod()) {
            return PREPENDED_TEXT_FOR_FILLING_DATE
                + DateFormatUtil.toTextFormat(observation.getEffectivePeriod().getStartElement());
        }
        return StringUtils.EMPTY;
    }

    private void buildNarrativeStatementForObservationComments(
        InstantType issuedElement,
        List<Observation> observations,
        StringBuilder reportLevelNarrativeStatements) {

        var narrativeStatementObservationComments = observations.stream()
            .filter(Observation::hasCode)
            .filter(DiagnosticReportMapper::isFilingComment)
            .filter(Observation::hasComment)
            .map(observation -> buildNarrativeStatementForDiagnosticReport(
                    issuedElement,
                    CommentType.USER_COMMENT.getCode(),
                    observation.getComment(),
                    confidentialityService.generateConfidentialityCode(observation).orElse(null)
                )
            )
            .collect(Collectors.joining(System.lineSeparator()));

        reportLevelNarrativeStatements.append(narrativeStatementObservationComments);
    }

    /**
     * See the
     * <a href="https://simplifier.net/guide/gpconnect-data-model/Home/FHIR-Assets/All-assets/Profiles/Profile--CareConnect-GPC-Observation-1?version=current">
     *  GP Connect 1.6.2
     * </a>
     * specification for more details on filing comments.
     * @param observation The Observation to check.
     * @return True if the Observation is a filing comment, otherwise false.
     */
    static boolean isFilingComment(Observation observation) {
        return observation.getCode().hasCoding()
            && observation.getCode().getCoding().stream()
            .filter(Coding::hasCode)
            .anyMatch(coding -> COMMENT_NOTE.equals(coding.getCode()));
    }

    private void buildNarrativeStatementForMissingResults(DiagnosticReport diagnosticReport, StringBuilder reportLevelNarrativeStatements) {
        if (reportLevelNarrativeStatements.isEmpty() && !diagnosticReport.hasResult()) {
            String narrativeStatementFromCodedDiagnosis = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport.getIssuedElement(), CommentType.AGGREGATE_COMMENT_SET.getCode(), "EMPTY REPORT", null
            );
            reportLevelNarrativeStatements.append(narrativeStatementFromCodedDiagnosis);
        }
    }

    private void buildNarrativeStatementForParticipants(DiagnosticReport diagnosticReport, StringBuilder reportLevelNarrativeStatements) {
        if (diagnosticReport.hasPerformer()) {
            var humanNames = buildListOfHumanReadableNames(diagnosticReport.getPerformer());
            String performerNarrativeStatement = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport.getIssuedElement(), CommentType.AGGREGATE_COMMENT_SET.getCode(),
                PREPENDED_TEXT_FOR_PARTICIPANTS + humanNames, null
            );
            reportLevelNarrativeStatements.append(performerNarrativeStatement);
        }
    }

    private String buildNarrativeStatementForDiagnosticReport(InstantType issuedElement,
                                                              String commentType,
                                                              String comment,
                                                              String confidentialityCode) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .commentType(commentType)
            .commentDate(DateFormatUtil.toHl7Format(issuedElement))
            .comment(comment)
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTime(issuedElement))
            .confidentialityCode(confidentialityCode);

        return TemplateUtils.fillTemplate(
            NARRATIVE_STATEMENT_TEMPLATE,
            narrativeStatementTemplateParameters.build()
        );
    }

    private String buildListOfHumanReadableNames(List<DiagnosticReport.DiagnosticReportPerformerComponent> performers) {
        return performers.stream()
            .map(DiagnosticReport.DiagnosticReportPerformerComponent::getActor)
            .map(this::fetchResource)
            .flatMap(Optional::stream)
            .map(this::fetchHumanNames)
            .collect(Collectors.joining(", "));
    }

    private Optional<Resource> fetchResource(Reference reference) {
        return messageContext.getInputBundleHolder().getResource(reference.getReferenceElement());
    }

    private String fetchHumanNames(Resource resource) {
        if (ResourceType.Practitioner.equals(resource.getResourceType())) {
            var practitionerName = ((Practitioner) resource).getNameFirstRep();
            return Stream.of(
                    practitionerName.getPrefixAsSingleString(),
                    practitionerName.getGivenAsSingleString(),
                    practitionerName.getFamily())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(StringUtils.SPACE));
        }
        if (ResourceType.Organization.equals(resource.getResourceType())) {
            return ((Organization) resource).getName();
        }
        return StringUtils.EMPTY;
    }

    private void markObservationsAsProcessed(IdMapper idMapper, List<Observation> observations) {
        observations.stream()
            .map(Observation::getIdElement)
            .forEach(idMapper::markObservationAsMapped);
    }

    private static boolean isPMIPCodeSystem(Identifier identifier) {
        return StringUtils
            .removeStart(identifier.getSystem(), URN_OID_PREFIX)
            .equals(PMIP_CODE_SYSTEM);
    }
}