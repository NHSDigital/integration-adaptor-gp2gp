package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class MessageContext {

    private static ThreadLocal<IdMapper> idMapperHolder = new NamedThreadLocal<>("IdMapper");

    @Autowired
    private RandomIdGeneratorService randomIdGeneratorService;

    public void resetMessageContext() {
        idMapperHolder.remove();
    }

    public IdMapper getIdMapper() {
        if (idMapperHolder.get() == null) {
            idMapperHolder.set(new IdMapper(randomIdGeneratorService));
        }

        return idMapperHolder.get();
    }
}
