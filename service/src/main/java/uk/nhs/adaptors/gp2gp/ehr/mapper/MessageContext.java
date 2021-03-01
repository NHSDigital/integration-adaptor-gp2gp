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

    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;

    public void resetMessageContext() {
        idMapperHolder.remove();
        inputBundleHolder.remove();
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
}
