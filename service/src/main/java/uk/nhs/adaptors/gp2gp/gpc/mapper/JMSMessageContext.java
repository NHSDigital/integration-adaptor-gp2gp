package uk.nhs.adaptors.gp2gp.gpc.mapper;

import org.springframework.core.NamedThreadLocal;

import uk.nhs.adaptors.gp2gp.gpc.mapper.IdMapper;

public class JMSMessageContext {

    private static ThreadLocal<IdMapper> jmsMessageContextHolder = new NamedThreadLocal<>("IdMapper");

    public static void resetJmsAttributes() {
        jmsMessageContextHolder.remove();
    }

    public static void setJmsAttributes(IdMapper idMapper) {
        if (idMapper == null) {
            resetJmsAttributes();
        }
        else {
            jmsMessageContextHolder.set(idMapper);
        }
    }

    public static IdMapper getJmsAttributes() {
        return jmsMessageContextHolder.get();
    }
}
