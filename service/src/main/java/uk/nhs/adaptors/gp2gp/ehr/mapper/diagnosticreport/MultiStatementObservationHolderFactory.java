package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import org.hl7.fhir.dstu3.model.Observation;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

@Component
@RequiredArgsConstructor
public class MultiStatementObservationHolderFactory {
    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public MultiStatementObservationHolder newInstance(Observation observation) {
        return new MultiStatementObservationHolder(observation, messageContext, randomIdGeneratorService);
    }
}
