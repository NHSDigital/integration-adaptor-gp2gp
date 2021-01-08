package uk.nhs.adaptors.gp2gp.common.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ConversationIdService {
    private static final String MDC_KEY = "ConversationId";

    public void applyConversationId(String id) {
        MDC.put(MDC_KEY, id);
    }

    public void resetConversationId() {
        MDC.remove(MDC_KEY);
    }

    public String getCurrentConversationId() {
        return MDC.get(MDC_KEY);
    }
}
