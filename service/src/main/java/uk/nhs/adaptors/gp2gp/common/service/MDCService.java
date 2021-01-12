package uk.nhs.adaptors.gp2gp.common.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MDCService {
    private static final String MDC_CONVERSATION_ID_KEY = "ConversationId";
    private static final String MDC_TASK_ID_KEY = "TaskId";

    public void applyConversationId(String id) {
        MDC.put(MDC_CONVERSATION_ID_KEY, id);
    }

    public void applyTaskId(String id) {
        MDC.put(MDC_TASK_ID_KEY, id);
    }

    public void resetAllMdcKeys() {
        MDC.clear();
    }
}
