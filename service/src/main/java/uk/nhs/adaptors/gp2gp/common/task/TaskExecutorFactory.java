package uk.nhs.adaptors.gp2gp.common.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public TaskExecutor getTaskExecutor(Class<? extends TaskDefinition> taskDefinitionClass) throws TaskHandlerException  {
        var taskExecutor = Optional.ofNullable(taskExecutorMap.get(taskDefinitionClass))
            .orElseThrow(() ->
                new TaskHandlerException("No task executor class for task definition class '" + taskDefinitionClass + "'"));

        return taskExecutor;
    }
}
