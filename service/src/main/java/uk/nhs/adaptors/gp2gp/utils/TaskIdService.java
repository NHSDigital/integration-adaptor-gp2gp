package uk.nhs.adaptors.gp2gp.utils;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TaskIdService {
    private static final String MDC_KEY = "TaskId";

    public void applyTaskId(String id) {
        MDC.put(MDC_KEY, id);
    }

    public void resetTaskId() {
        MDC.remove(MDC_KEY);
    }

    public String getCurrentTaskId() {
        return MDC.get(MDC_KEY);
    }
}
