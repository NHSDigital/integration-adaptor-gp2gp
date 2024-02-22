package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AbsentAttachmentUtils {

    public static String buildAbsentAttachmentFileName(String documentId) {
        return "AbsentAttachment" + documentId + ".txt";
    }

}
