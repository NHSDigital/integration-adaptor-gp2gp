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
    // but I see now way of changing order of existing file extensions
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
        var fileExtension = mapContentTypeToFileExtension(extractContentType(attachment));

        return Optional.ofNullable(attachment.getTitle())
            .map(__ -> "AbsentAttachment" + narrativeStatementId + fileExtension)
            .orElse(narrativeStatementId + "_" + narrativeStatementId + fileExtension);
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
