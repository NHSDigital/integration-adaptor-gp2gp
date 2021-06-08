package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class MessageContext {

    private static ThreadLocal<IdMapper> idMapperHolder = new NamedThreadLocal<>("IdMapper");
    private static ThreadLocal<InputBundle> inputBundleHolder = new NamedThreadLocal<>("InputBundle");
    private static ThreadLocal<MedicationRequestIdMapper> medicationRequestIdHolder = new NamedThreadLocal<>("MedicationRequestIdMapper");
    private static ThreadLocal<EhrFolderEffectiveTime> ehrFolderEffectiveTimeHolder = new NamedThreadLocal<>("EhrFolderEffectiveTime");
    private static ThreadLocal<AgentDirectory> agentDirectoryHolder = new NamedThreadLocal<>("AgentDirectory");


    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;

    public void resetMessageContext() {
        idMapperHolder.remove();
        inputBundleHolder.remove();
        medicationRequestIdHolder.remove();
        ehrFolderEffectiveTimeHolder.remove();
        agentDirectoryHolder.remove();
    }

    public void initialize(Bundle bundle) {
        inputBundleHolder.set(new InputBundle(bundle));
        agentDirectoryHolder.set(new AgentDirectory(randomIdGeneratorService, bundle));
    }

    public IdMapper getIdMapper() {
        if (idMapperHolder.get() == null) {
            idMapperHolder.set(new IdMapper(randomIdGeneratorService));
        }

        return idMapperHolder.get();
    }


    public AgentDirectory getAgentDirectory() {
        return agentDirectoryHolder.get();
    }

    public EhrFolderEffectiveTime getEffectiveTime() {
        if (ehrFolderEffectiveTimeHolder.get() == null) {
            ehrFolderEffectiveTimeHolder.set(new EhrFolderEffectiveTime());
        }

        return ehrFolderEffectiveTimeHolder.get();
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
}
