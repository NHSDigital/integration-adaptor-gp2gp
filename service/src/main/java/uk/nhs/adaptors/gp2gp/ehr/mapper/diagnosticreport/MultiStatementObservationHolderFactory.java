package uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport;

import org.hl7.fhir.dstu3.model.Observation;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiStatementObservationHolderFactory {
    private final MessageContext messageContext;
    private final RandomIdGeneratorService randomIdGeneratorService;

    public MultiStatementObservationHolder newInstance(Observation observation) {
        return new MultiStatementObservationHolder(observation);
    }

    /**
     * Some Observation map into more than one XStatement in the EHR. The first
     * XStatement must have and id mapped (via IdMapper) to the Observation and
     * additional statements have random / unmapped ids.
     */
    public class MultiStatementObservationHolder {
        private final Observation observation;
        private String singleUseIdentifier;

        MultiStatementObservationHolder(Observation observation) {
            this.observation = observation;
            singleUseIdentifier = messageContext.getIdMapper()
                .getOrNew(observation.getResourceType(), observation.getIdElement());
        }

        public Observation getObservation() {
            return this.observation;
        }

        public String nextHl7InstanceIdentifier() {
            if (singleUseIdentifier != null) {
                LOGGER.debug("HL7 II {} used for the primary statement mapping of {}",
                    singleUseIdentifier, observation.getId());
                var identifier =  singleUseIdentifier;
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

}
