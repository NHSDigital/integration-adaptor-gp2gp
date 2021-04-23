package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnostic_report;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.mapper.IdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.DiagnosticReportCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.ObservationCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnostic_report.SpecimenCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class DiagnosticReportMapper {

    private static final Mustache DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("diagnostic_report_compound_statement_template.mustache");
    private static final Mustache SPECIMEN_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("specimen_compound_statement_template.mustache");
    private static final Mustache OBSERVATION_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("observation_compound_statement_template.mustache");
    private static final String EFFECTIVE_TIME_TEMPLATE = "<effectiveTime><center value=\"%s\"></effectiveTime>";

    private final MessageContext messageContext;

    private final ObservationMapper observationMapper;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport) {
        final IdMapper idMapper = messageContext.getIdMapper();

        List<Specimen> specimens = fetchSpecimens(diagnosticReport);

        List<Observation> observations = fetchObservations(diagnosticReport);

        StringBuilder specimensBlock = new StringBuilder();
        specimens.forEach(specimen -> {
            List<Observation> observationsAssociatedWithSpecimen = observations.stream()
                .filter(Observation::hasSpecimen)
                .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
                .collect(Collectors.toList());

            StringBuilder observationsBlock = new StringBuilder();
            observationsAssociatedWithSpecimen.forEach(observationAssociatedWithSpecimen -> {
                List<Observation> derivedObservations = observations.stream()
                    .filter(observation ->
                        observation.getRelated().stream().anyMatch(
                            observationRelation -> observationRelation.getType() == Observation.ObservationRelationshipType.DERIVEDFROM &&
                                observationRelation.getTarget().getReference().equals(observationAssociatedWithSpecimen.getId())
                        )
                    )
                    .collect(Collectors.toList());

                String observationStatement = observationMapper.mapObservationToObservationStatement(observationAssociatedWithSpecimen);

                StringBuilder narrativeStatementsBlock = new StringBuilder();

                if (observationAssociatedWithSpecimen.hasComment()) {
                    narrativeStatementsBlock.append(
                        observationMapper.mapObservationToNarrativeStatement(observationAssociatedWithSpecimen)
                    );
                }

                derivedObservations.forEach(derivedObservation -> {
                    if (derivedObservation.hasComment()) {
                        narrativeStatementsBlock.append(
                            observationMapper.mapObservationToNarrativeStatement(derivedObservation)
                        );
                    }
                });

                var observationCompoundStatementTemplateParameters = ObservationCompoundStatementTemplateParameters.builder()
                    .compoundStatementId(
                        idMapper.getOrNew(ResourceType.Observation, observationAssociatedWithSpecimen.getId())
                    )
                    .observationStatement(observationStatement)
                    .narrativeStatements(narrativeStatementsBlock.toString());

                observationsBlock.append(
                    TemplateUtils.fillTemplate(
                        OBSERVATION_COMPOUND_STATEMENT_TEMPLATE,
                        observationCompoundStatementTemplateParameters.build()
                    )
                );
            });

            var specimenCompoundStatementTemplateParameters = SpecimenCompoundStatementTemplateParameters.builder()
                .compoundStatementId(idMapper.getOrNew(ResourceType.Specimen, specimen.getId()))
                .diagnosticReportIssuedDate(DateFormatUtil.toHl7Format(diagnosticReport.getIssuedElement()))
                .accessionIdentifier(specimen.getAccessionIdentifier().getValue())
                .effectiveTime(generateEffectiveTimeElement(specimen))
                .observations(observationsBlock.toString());

            if (specimen.getType().hasCoding()) {
                specimen.getType().getCoding().stream()
                    .findFirst()
                    .ifPresent(coding ->
                        specimenCompoundStatementTemplateParameters.type(
                            coding.getDisplay()
                        )
                    );
            } else if (specimen.getType().hasText())  {
                specimenCompoundStatementTemplateParameters.type(specimen.getType().getText());
            }

            specimensBlock.append(
                TemplateUtils.fillTemplate(
                    SPECIMEN_COMPOUND_STATEMENT_TEMPLATE,
                    specimenCompoundStatementTemplateParameters.build()
                )
            );
        });

        var diagnosticReportCompoundStatementTemplateParameters = DiagnosticReportCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.DiagnosticReport, diagnosticReport.getId()))
            .diagnosticReportIssuedDate(DateFormatUtil.toHl7Format(diagnosticReport.getIssuedElement()))
            .specimens(specimensBlock.toString());

        String temp = TemplateUtils.fillTemplate(
            DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE,
            diagnosticReportCompoundStatementTemplateParameters.build()
        );

        return temp;
    }

    private List<Specimen> fetchSpecimens(DiagnosticReport diagnosticReport) {
        return diagnosticReport.getSpecimen()
            .stream()
            .map(specimenReference -> messageContext.getInputBundleHolder().getResource(specimenReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Specimen.class::cast)
            .collect(Collectors.toList());
    }

    private List<Observation> fetchObservations(DiagnosticReport diagnosticReport) {
        return diagnosticReport.getResult()
            .stream()
            .map(observationReference -> messageContext.getInputBundleHolder().getResource(observationReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Observation.class::cast)
            .collect(Collectors.toList());
    }

    private String generateEffectiveTimeElement(Specimen specimen) {
        if (specimen.getCollection().hasCollectedDateTimeType()) {
            return String.format(
                EFFECTIVE_TIME_TEMPLATE,
                DateFormatUtil.toHl7Format(specimen.getCollection().getCollectedDateTimeType())
            );
        }

        if (specimen.hasReceivedTime()) {
            return String.format(
                EFFECTIVE_TIME_TEMPLATE,
                DateFormatUtil.toHl7Format(specimen.getReceivedTimeElement())
            );
        }

        return StringUtils.EMPTY;
    }
}
