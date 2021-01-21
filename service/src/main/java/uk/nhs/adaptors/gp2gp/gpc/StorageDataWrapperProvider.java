package uk.nhs.adaptors.gp2gp.gpc;

import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;

public final class StorageDataWrapperProvider {
    private StorageDataWrapperProvider() {
    }

    public static StorageDataWrapper buildStorageDataWrapper(TaskDefinition taskDefinition,
            String response,
            String taskId) {
        return StorageDataWrapper.builder()
            .type(taskDefinition.getTaskType().getTaskTypeHeaderValue())
            .conversationId(taskDefinition.getConversationId())
            .taskId(taskId)
            .response(response)
            .build();
    }
}
