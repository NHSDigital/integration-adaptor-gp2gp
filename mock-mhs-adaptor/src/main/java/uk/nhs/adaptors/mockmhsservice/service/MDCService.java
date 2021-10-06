package uk.nhs.adaptors.mockmhsservice.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MDCService {
    private static final String MDC_CONVERSATION_ID_KEY = "ConversationId";

    public void applyConversationId(String id) {
        MDC.put(MDC_CONVERSATION_ID_KEY, id);
    }

    public void resetAllMdcKeys() {
        MDC.clear();
    }
}