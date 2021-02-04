package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.springframework.core.NamedThreadLocal;

public class MessageContext {

    private static ThreadLocal<IdMapper> idMapperHolder = new NamedThreadLocal<>("IdMapper");

    public static void resetMessageContext() {
        idMapperHolder.remove();
    }

    public static void setMessageContext(IdMapper idMapper) {
        if (idMapper == null) {
            resetMessageContext();
        } else {
            idMapperHolder.set(idMapper);
        }
    }

    public static IdMapper getIdMapper() {
        return idMapperHolder.get();
    }
}
