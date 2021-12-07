package uk.nhs.adaptors.gp2gp.ehr.utils;

public class AbsentAttachmentUtils {

    public static String buildAbsentAttachmentFileName(String conversationId, String documentId) {
        return "AbsentAttachment" + conversationId + "/" + documentId + ".txt";
    }

}
