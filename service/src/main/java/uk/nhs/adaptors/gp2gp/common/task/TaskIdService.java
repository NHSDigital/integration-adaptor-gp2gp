package uk.nhs.adaptors.gp2gp.common.task;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskIdService {
    private static final String MDC_KEY = "TaskId";

    public String createNewTaskId() {
        return UUID.randomUUID().toString();
    }

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
