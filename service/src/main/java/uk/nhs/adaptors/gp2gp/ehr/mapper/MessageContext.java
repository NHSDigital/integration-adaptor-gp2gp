package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

import java.util.Optional;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class MessageContext {

    private static ThreadLocal<IdMapper> idMapperHolder = new NamedThreadLocal<>("IdMapper");
    private static ThreadLocal<InputBundle> inputBundleHolder = new NamedThreadLocal<>("InputBundle");
    private static ThreadLocal<MedicationRequestIdMapper> medicationRequestIdHolder = new NamedThreadLocal<>("MedicationRequestIdMapper");
    private static ThreadLocal<String> agentReference = new NamedThreadLocal<>("AgentReference");

    @Autowired
    private final RandomIdGeneratorService randomIdGeneratorService;

    public void resetMessageContext() {
        idMapperHolder.remove();
        inputBundleHolder.remove();
        medicationRequestIdHolder.remove();
        agentReference.remove();
    }

    public void initialize(Bundle bundle) {
        inputBundleHolder.set(new InputBundle(bundle));
    }

    public IdMapper getIdMapper() {
        if (idMapperHolder.get() == null) {
            idMapperHolder.set(new IdMapper(randomIdGeneratorService));
        }

        return idMapperHolder.get();
    }

    public InputBundle getInputBundleHolder() {
        return inputBundleHolder.get();
    }

    public MedicationRequestIdMapper getMedicationRequestIdMapper() {
        if (medicationRequestIdHolder.get() == null) {
            medicationRequestIdHolder.set(new MedicationRequestIdMapper(randomIdGeneratorService));
        }

        return medicationRequestIdHolder.get();
    }

    public void setAgentReference(String reference) {
        agentReference.set(reference);
    }

    public Optional<String> getAgentReference() {
        return Optional.ofNullable(agentReference.get());
    }
}
