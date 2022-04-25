package uk.nhs.adaptors.gp2gp.ehr.utils;

public class AbsentAttachmentUtils {

    public static String buildAbsentAttachmentFileName(String documentId) {
        return "AbsentAttachment" + documentId + ".txt";
    }

}
