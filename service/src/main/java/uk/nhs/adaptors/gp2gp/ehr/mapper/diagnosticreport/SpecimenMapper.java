package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

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
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.diagnosticreport.SpecimenCompoundStatementTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SpecimenMapper {

    private static final Mustache SPECIMEN_COMPOUND_STATEMENT_TEMPLATE =
        TemplateUtils.loadTemplate("specimen_compound_statement_template.mustache");
    private static final String EFFECTIVE_TIME_TEMPLATE = "<effectiveTime><center value=\"%s\"></effectiveTime>";

    private final MessageContext messageContext;
    private final ObservationMapper observationMapper;

    public String mapSpecimenToCompoundStatement(Specimen specimen, List<Observation> observations, DiagnosticReport diagnosticReport) {
        List<Observation> observationsAssociatedWithSpecimen = observations.stream()
            .filter(Observation::hasSpecimen)
            .filter(observation -> observation.getSpecimen().getReference().equals(specimen.getId()))
            .collect(Collectors.toList());

        StringBuilder observationsBlock = new StringBuilder();
        observationsAssociatedWithSpecimen.forEach(observationAssociatedWithSpecimen -> {
            observationsBlock.append(
                observationMapper.mapObservationToCompoundStatement(
                    observationAssociatedWithSpecimen,
                    observations
                )
            );
        });

        final IdMapper idMapper = messageContext.getIdMapper();

        var specimenCompoundStatementTemplateParameters = SpecimenCompoundStatementTemplateParameters.builder()
            .compoundStatementId(idMapper.getOrNew(ResourceType.Specimen, specimen.getId()))
            .diagnosticReportIssuedDate(DateFormatUtil.toHl7Format(diagnosticReport.getIssuedElement()))
            .accessionIdentifier(specimen.getAccessionIdentifier().getValue())
            .effectiveTime(prepareEffectiveTimeElement(specimen))
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

        return TemplateUtils.fillTemplate(
            SPECIMEN_COMPOUND_STATEMENT_TEMPLATE,
            specimenCompoundStatementTemplateParameters.build()
        );
    }

    private String prepareEffectiveTimeElement(Specimen specimen) {
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
