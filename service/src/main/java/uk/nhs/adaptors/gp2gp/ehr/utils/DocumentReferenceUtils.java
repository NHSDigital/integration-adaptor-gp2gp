package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.DocumentReference;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.util.Optional;

public final class DocumentReferenceUtils {

    private DocumentReferenceUtils() {
    }

    public static Attachment extractAttachment(DocumentReference documentReference) {
        return documentReference.getContent()
            .stream()
            .findFirst()
            .filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
            .map(DocumentReference.DocumentReferenceContentComponent::getAttachment)
            .orElseThrow(() -> new EhrMapperException("documentReference.content[0] is missing an attachment"));
    }

    public static String extractContentType(Attachment attachment) {
        return Optional.ofNullable(attachment.getContentType())
            .orElseThrow(() -> new EhrMapperException("documentReference.content[0].attachment is missing contentType"));
    }

    public static String buildAttachmentFileName(String narrativeStatementId, Attachment attachment) {
        var fileExtension = mapContentTypeToFileExtension(extractContentType(attachment));

        return Optional.ofNullable(attachment.getTitle())
            .map(__ -> "AbsentAttachment" + narrativeStatementId + fileExtension)
            .orElse(narrativeStatementId + "_" + narrativeStatementId + fileExtension);
    }

    private static String mapContentTypeToFileExtension(String contentType) {
        //TODO: https://gpitbjss.atlassian.net/browse/NIAD-1056
        return ".xyz";
    }
}
