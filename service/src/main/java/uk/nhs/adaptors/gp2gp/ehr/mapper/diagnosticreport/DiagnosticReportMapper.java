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

import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantType;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.DiagnosticReportCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class DiagnosticReportMapper {

    private static final Mustache DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("diagnostic_report_compound_statement_template.mustache");

    private final MessageContext messageContext;
    private final SpecimenMapper specimenMapper;
    private final ParticipantMapper participantMapper;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport) {
        List<Specimen> specimens = fetchSpecimens(diagnosticReport);
        List<Observation> observations = fetchObservations(diagnosticReport);

        String diagnosticReportIssuedDate = DateFormatUtil.toHl7Format(diagnosticReport.getIssuedElement());

        String mappedSpecimens = specimens.stream()
            .map(specimen -> specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, diagnosticReportIssuedDate))
            .collect(Collectors.joining());

        final IdMapper idMapper = messageContext.getIdMapper();

        var diagnosticReportCompoundStatementTemplateParameters = DiagnosticReportCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.DiagnosticReport, diagnosticReport.getId()))
            .diagnosticReportIssuedDate(diagnosticReportIssuedDate)
            .specimens(mappedSpecimens);

        if (diagnosticReport.hasPerformer() && diagnosticReport.getPerformerFirstRep().hasActor()) {
            final String participantReference = idMapper.get(diagnosticReport.getPerformerFirstRep().getActor());
            final String participantBlock = participantMapper.mapToParticipant(participantReference, ParticipantType.AUTHOR);
            diagnosticReportCompoundStatementTemplateParameters.participant(participantBlock);
        }

        return TemplateUtils.fillTemplate(
            DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE,
            diagnosticReportCompoundStatementTemplateParameters.build()
        );
    }

    private List<Specimen> fetchSpecimens(DiagnosticReport diagnosticReport) {
        if (diagnosticReport.getSpecimen().isEmpty()) {
            return Collections.singletonList(generateDefaultSpecimen(diagnosticReport));
        }

        return diagnosticReport.getSpecimen()
            .stream()
            .map(specimenReference -> messageContext.getInputBundleHolder().getResource(specimenReference.getReferenceElement()))
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
        if (diagnosticReport.getResult().isEmpty()) {
            return Collections.singletonList(generateDefaultObservation(diagnosticReport));
        }

        return diagnosticReport.getResult()
            .stream()
            .map(observationReference -> messageContext.getInputBundleHolder().getResource(observationReference.getReferenceElement()))
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
}
