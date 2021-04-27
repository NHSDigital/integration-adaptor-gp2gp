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

    private final MessageContext messageContext;
    private final SpecimenMapper specimenMapper;

    public String mapDiagnosticReportToCompoundStatement(DiagnosticReport diagnosticReport) {
        List<Specimen> specimens = fetchSpecimens(diagnosticReport);
        List<Observation> observations = fetchObservations(diagnosticReport);

        StringBuilder specimensBlock = new StringBuilder();
        specimens.forEach(specimen ->
            specimensBlock.append(
                specimenMapper.mapSpecimenToCompoundStatement(specimen, observations, diagnosticReport)
            )
        );

        final IdMapper idMapper = messageContext.getIdMapper();

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
}
