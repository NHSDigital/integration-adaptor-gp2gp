package uk.nhs.adaptors.gp2gp.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskExecutorFactory {

    private final Map<Class<? extends TaskDefinition>, TaskExecutor> taskExecutorMap;

    @Autowired
    public TaskExecutorFactory(List<TaskExecutor> taskExecutorList) {
        taskExecutorMap = taskExecutorList.stream()
                .collect(Collectors.toMap(TaskExecutor::getTaskType, Function.identity()));
    }

    public TaskExecutor getTaskExecutor(Class<? extends TaskDefinition> taskType) {
        return taskExecutorMap.get(taskType);
    }
}
