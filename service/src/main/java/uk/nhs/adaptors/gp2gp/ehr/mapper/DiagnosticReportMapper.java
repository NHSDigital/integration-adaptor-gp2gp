package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.DiagnosticReportCompoundStatementTemplateParameters;
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

    private final MessageContext messageContext;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport, boolean isNested) {
        final IdMapper idMapper = messageContext.getIdMapper();

        List<Specimen> specimens = diagnosticReport.getSpecimen().stream()
            .map(specimenReference -> messageContext.getInputBundleHolder().getResource(specimenReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Specimen.class::cast)
            .collect(Collectors.toList());

        List<Observation> observations = diagnosticReport.getResult().stream()
            .map(observationReference -> messageContext.getInputBundleHolder().getResource(observationReference.getReferenceElement()))
            .flatMap(Optional::stream)
            .map(Observation.class::cast)
            .collect(Collectors.toList());

        var diagnosticReportCompoundStatementTemplateParameters = DiagnosticReportCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.DiagnosticReport, diagnosticReport.getId()));

        specimens.forEach(specimen -> {
            List<Observation> observationsAssociatedWithSpecimen = observations.stream()
                .filter(Observation::hasSpecimen)
                .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
                .collect(Collectors.toList());

            observationsAssociatedWithSpecimen.forEach(observationAssociatedWithSpecimen -> {
                List<Observation> relatedObservations = observations.stream()
                    .filter(observation ->
                        observation.getRelated().stream().anyMatch(
                            observationRelation -> observationRelation.getType() == Observation.ObservationRelationshipType.DERIVEDFROM &&
                                observationRelation.getTarget().getReference().equals(observationAssociatedWithSpecimen.getId())
                        )
                    )
                    .collect(Collectors.toList());

                System.out.println(specimen);
                System.out.println(observationsAssociatedWithSpecimen);
                System.out.println(relatedObservations);
            });
        });

        return "";

//        return TemplateUtils.fillTemplate(
//            DIAGNOSTIC_REPORT_COMPOUND_STATEMENT_TEMPLATE,
//            diagnosticReportCompoundStatementTemplateParameters.build()
//        );
    }
}
