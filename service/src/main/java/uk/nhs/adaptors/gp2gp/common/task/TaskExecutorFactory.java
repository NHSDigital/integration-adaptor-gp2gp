package uk.nhs.adaptors.gp2gp.common.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@SuppressWarnings({"unchecked", "rawtypes"})
public class TaskExecutorFactory {

    private final Map<Class<? extends TaskDefinition>, TaskExecutor> taskExecutorMap;

    @Autowired
    public TaskExecutorFactory(List<TaskExecutor> taskExecutorList) {
        taskExecutorMap = taskExecutorList.stream()
                .collect(Collectors.toMap(TaskExecutor::getTaskType, Function.identity()));
    }

    public TaskExecutor getTaskExecutor(Class<? extends TaskDefinition> taskDefinitionClass) throws TaskHandlerException  {
        return Optional.ofNullable(taskExecutorMap.get(taskDefinitionClass))
            .orElseThrow(() ->
                new TaskHandlerException("No task executor class for task definition class '" + taskDefinitionClass + "'"));
    }
}
