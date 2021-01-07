package uk.nhs.adaptors.gp2gp.utils;

public final class JmsHeaders {
    private static String conversationId = "ConversationId";
    private static String taskId = "TaskId";

    public static String getConversationIdHeader() {
        return conversationId;
    }

    public static String getTaskIdHeader() {
        return taskId;
    }
}
