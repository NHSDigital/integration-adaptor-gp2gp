package uk.nhs.adaptors.gp2gp.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutor;

public class TaskService {
    public void handleRequest(TaskDefinition taskDefinition) {
        Map<Class<? extends TaskDefinition>, TaskExecutor> taskExecutorMap = createExecutors(taskDefinition);
        TaskExecutor taskExecutor = taskExecutorMap.get(taskDefinition.getClass());
        taskExecutor.execute(taskDefinition);
    }

    private Map<Class<? extends TaskDefinition>, TaskExecutor> createExecutors(TaskDefinition taskDefinition) {
        Optional<TaskExecutor> taskExecutor = Optional.empty();
        Map<Class<? extends TaskDefinition>, TaskExecutor> taskExecutorMap = new HashMap<>();

        if (taskDefinition.getClass() == GetGpcStructuredTaskDefinition.class) {
            taskExecutor = Optional.of(new GetGpcStructuredTaskExecutor());
        }
        if (taskDefinition.getClass() == GetGpcDocumentTaskDefinition.class) {
            taskExecutor = Optional.of(new GetGpcDocumentTaskExecutor());
        }
        if (taskExecutor.isPresent()) {
            taskExecutorMap.put(taskExecutor.get().getTaskType(), taskExecutor.get());
        }

        return taskExecutorMap;
    }

}
