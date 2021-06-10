package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import org.hl7.fhir.dstu3.model.Observation;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

/**
 * Some Observation map into more than one XStatement in the EHR. The first
 * XStatement has an id mapped (via IdMapper) to the Observation and
 * additional statements have random / unmapped ids.
 */
@Slf4j
class MultiStatementObservationHolder {
    private final Observation observation;
    private String singleUseIdentifier;
    private final RandomIdGeneratorService randomIdGeneratorService;

    MultiStatementObservationHolder(Observation observation, MessageContext messageContext,
        RandomIdGeneratorService randomIdGeneratorService) {
        this.observation = observation;
        singleUseIdentifier = messageContext.getIdMapper()
            .getOrNew(observation.getResourceType(), observation.getIdElement());
        this.randomIdGeneratorService = randomIdGeneratorService;
    }

    Observation getObservation() {
        return this.observation;
    }

    String nextHl7InstanceIdentifier() {
        if (singleUseIdentifier != null) {
            LOGGER.debug("HL7 II {} used for the primary statement mapping of {}",
                singleUseIdentifier, observation.getId());
            var identifier = singleUseIdentifier;
            singleUseIdentifier = null;
            return identifier;
        }
        var randomId = randomIdGeneratorService.createNewId();
        LOGGER.debug("HL7 II {} used for an additional statement mapping of {}",
            randomId, observation.getId());
        return randomId;
    }

    void verifyObservationWasMapped() {
        if (singleUseIdentifier != null) {
            throw new EhrMapperException(observation.getId() + " was not mapped to a statement in the EHR");
        }
    }
}
