package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.DocumentReference;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.util.Optional;

public final class DocumentReferenceUtils {

    private DocumentReferenceUtils() {
    }

    // By default tika is using .mpga as default extension for audio/mpeg
    // but we need .mp3 to be the default one.
    // There is a way to add custom media types in a custom-mimetypes.xml
    // but I see no way of changing order of existing file extensions
    // Because of that all extensions have been copied from the original tika project
    // https://raw.githubusercontent.com/apache/tika/master/tika-core/src/main/resources/org/apache/tika/mime/tika-mimetypes.xml
    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

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
        String contentType = extractContentType(attachment);

        return Optional.ofNullable(attachment.getTitle())
            .map(__ -> buildMissingAttachmentFileName(narrativeStatementId))
            .orElse(buildPresentAttachmentFileName(narrativeStatementId, contentType));
    }

    public static String buildPresentAttachmentFileName(String narrativeStatementId, String contentType) {
        var fileExtension = mapContentTypeToFileExtension(contentType);

        //Originally this was like this
        //  narrativeStatementId + "_" + narrativeStatementId + fileExtension;
        //but I can't find any documentation why so I've decided to make this
        //  narrativeStatementId + fileExtension
        //so that it matches examples we were given
        return narrativeStatementId + fileExtension;
    }

    public static String buildMissingAttachmentFileName(String narrativeStatementId) {
        return "AbsentAttachment" + narrativeStatementId + ".txt";
    }

    private static String mapContentTypeToFileExtension(String contentType) {
        String extension;
        try {
            extension = MIME_TYPES.forName(contentType).getExtension();
        } catch (MimeTypeException e) {
            throw new EhrMapperException("Unhandled exception while parsing Content-Type: " + contentType, e);
        }
        if (StringUtils.isBlank(extension)) {
            throw new EhrMapperException("Unsupported Content-Type: " + contentType);
        }
        return extension;
    }
}
