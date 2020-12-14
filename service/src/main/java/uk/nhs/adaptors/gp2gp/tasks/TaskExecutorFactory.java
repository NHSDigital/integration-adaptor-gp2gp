package uk.nhs.adaptors.gp2gp.tasks;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskExecutorFactory {
    private static final Map<String, TaskExecutor> TASK_EXECUTOR_MAP;

    static {
        TASK_EXECUTOR_MAP = Map.of(GetGpcDocumentTaskDefinition.class.toString(), new GetGpcDocumentTaskExecutor(),
            GetGpcStructuredTaskDefinition.class.toString(), new GetGpcStructuredTaskExecutor());
    }

    public TaskExecutor getTaskExecutor(TaskDefinition taskDefinition) {
        return TASK_EXECUTOR_MAP.get(taskDefinition.getClass().toString());
    }
}
