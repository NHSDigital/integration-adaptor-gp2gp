package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper.NARRATIVE_STATEMENT_TEMPLATE;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class DiagnosticReportMapper {

    private static final Mustache DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("diagnostic_report_compound_statement_template.mustache");

    private static final String PREPENDED_TEXT_FOR_CONCLUSION_COMMENT = "Interpretation: ";
    private static final String PREPENDED_TEXT_FOR_CODED_DIAGNOSIS = "Lab Diagnosis: ";

    private final MessageContext messageContext;
    private final SpecimenMapper specimenMapper;
    private final ParticipantMapper participantMapper;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport) {
        List<Specimen> specimens = fetchSpecimens(diagnosticReport);
        List<Observation> observations = fetchObservations(diagnosticReport);

        String diagnosticReportIssuedDate = DateFormatUtil.toHl7Format(diagnosticReport.getIssuedElement());

        String mappedSpecimens = specimens.stream()
            .map(specimen -> specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, diagnosticReportIssuedDate))
            .collect(Collectors.joining());

        final IdMapper idMapper = messageContext.getIdMapper();

        String reportLevelNarrativeStatements = prepareReportLevelNarrativeStatements(diagnosticReport);

        var diagnosticReportCompoundStatementTemplateParameters = DiagnosticReportCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.DiagnosticReport, diagnosticReport.getIdElement()))
            .availabilityTime(diagnosticReportIssuedDate)
            .narrativeStatements(reportLevelNarrativeStatements)
            .specimens(mappedSpecimens);

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

    private List<Specimen> fetchSpecimens(DiagnosticReport diagnosticReport) {
        if (!diagnosticReport.hasSpecimen()) {
            return Collections.singletonList(generateDefaultSpecimen(diagnosticReport));
        }

        var inputBundleHolder = messageContext.getInputBundleHolder();
        return diagnosticReport.getSpecimen()
            .stream()
            .map(specimenReference -> inputBundleHolder.getResource(specimenReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Specimen.class::cast)
            .collect(Collectors.toList());
    }

    private Specimen generateDefaultSpecimen(DiagnosticReport diagnosticReport) {
        Specimen specimen = new Specimen();

        specimen.setId("Specimen/Default-1");

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

        observation.setId("Observation/Default-1");

        return observation
            .setSpecimen(new Reference().setReference("Specimen/Default-1"))
            .setIssuedElement(diagnosticReport.getIssuedElement())
            .setComment("EMPTY REPORT");
    }

    private String prepareReportLevelNarrativeStatements(DiagnosticReport diagnosticReport) {
        StringBuilder reportLevelNarrativeStatements = new StringBuilder();

        if (diagnosticReport.hasConclusion()) {
            String comment = PREPENDED_TEXT_FOR_CONCLUSION_COMMENT + diagnosticReport.getConclusion();

            String narrativeStatementFromConclusion = buildNarrativeStatementForDiagnosticReport(
                diagnosticReport, comment
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
                diagnosticReport, comment
            );

            reportLevelNarrativeStatements.append(narrativeStatementFromCodedDiagnosis);
        }

        return reportLevelNarrativeStatements.toString();
    }

    private String buildNarrativeStatementForDiagnosticReport(DiagnosticReport diagnosticReport, String comment) {
        var narrativeStatementTemplateParameters = NarrativeStatementTemplateParameters.builder()
            .narrativeStatementId(randomIdGeneratorService.createNewId())
            .commentType(CommentType.LABORATORY_RESULT_COMMENT.getCode())
            .issuedDate(DateFormatUtil.toHl7Format(diagnosticReport.getIssued().toInstant()))
            .comment(comment)
            .availabilityTimeElement(StatementTimeMappingUtils.prepareAvailabilityTimeForDiagnosticReport(diagnosticReport));

        return TemplateUtils.fillTemplate(
            NARRATIVE_STATEMENT_TEMPLATE,
            narrativeStatementTemplateParameters.build()
        );
    }
}
