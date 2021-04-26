package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.apache.tika.mime.MimeTypeException;
import org.hl7.fhir.dstu3.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentReferenceUtilsTest {
    private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
    private static final String NARRATIVE_STATEMENT_ID = "3b24b89b-fd14-49f9-ba12-3b4212b60080";
    private static final String FILE_MISSING_ATTACHMENT_TITLE = "Reason why file is missing";
    private static final String UNSUPPORTED_CONTENT_TYPE = "unknown/unknown";
    private static final String INVALID_CONTENT_TYPE = "invalid content type";

    @ParameterizedTest
    @CsvSource({
        "text/plain,.txt",
        "text/html,.html",
        "application/pdf,.pdf",
        "text/xml,.xml",
        "application/xml,.xml",
        "text/rtf,.rtf",
        "audio/mpeg,.mp3",
        "image/png,.png",
        "image/gif,.gif",
        "image/jpeg,.jpg",
        "image/tiff,.tiff",
        "video/mpeg,.mpeg",
        "application/msword,.doc",
        "application/octet-stream,.bin",
        "audio/basic,.au",
        "audio/x-au,.au",
    })
    void When_SupportedMimeTypeIsProvided_Expect_CorrectFileExtensionIsGenerated(String contentType, String fileExtension) {
        var fileName = DocumentReferenceUtils.buildAttachmentFileName(
            NARRATIVE_STATEMENT_ID, new Attachment().setContentType(contentType));

        assertThat(fileName).endsWith(fileExtension);
    }

    @Test
    void When_UnsupportedMimeTypeIsProvided_Expect_Exception() {
        assertThatThrownBy(() -> DocumentReferenceUtils.buildAttachmentFileName(
            NARRATIVE_STATEMENT_ID, new Attachment().setContentType(UNSUPPORTED_CONTENT_TYPE)))
            .isInstanceOf(EhrMapperException.class)
            .hasMessage("Unsupported Content-Type: unknown/unknown");
    }

    @Test
    void When_InvalidMimeTypeIsProvided_Expect_Exception() {
        assertThatThrownBy(() -> DocumentReferenceUtils.buildAttachmentFileName(
            NARRATIVE_STATEMENT_ID, new Attachment().setContentType(INVALID_CONTENT_TYPE)))
            .isInstanceOf(EhrMapperException.class)
            .hasMessage("Unhandled exception while parsing Content-Type: invalid content type")
            .hasCauseInstanceOf(MimeTypeException.class);
    }

    @Test
    void When_ExtractingContentType_Expect_AttachmentContentTypeIsReturned() {
        var attachment = new Attachment().setContentType(TEXT_PLAIN_CONTENT_TYPE);
        assertThat(DocumentReferenceUtils.extractContentType(attachment))
            .isEqualTo(TEXT_PLAIN_CONTENT_TYPE);
    }

    @Test
    void When_ExtractingMissingContentType_Expect_Exception() {
        var attachment = new Attachment();
        assertThatThrownBy(() -> DocumentReferenceUtils.extractContentType(attachment))
            .isInstanceOf(EhrMapperException.class)
            .hasMessage("documentReference.content[0].attachment is missing contentType");
    }

    @Test
    void When_BuildingFileNameIfTitleIsPresent_Expect_MissingAttachmentFileNameIsGenerated() {
        var attachment = new Attachment()
            .setContentType(TEXT_PLAIN_CONTENT_TYPE)
            .setTitle(FILE_MISSING_ATTACHMENT_TITLE);

        assertThat(DocumentReferenceUtils.buildAttachmentFileName(NARRATIVE_STATEMENT_ID, attachment))
            .isEqualTo("AbsentAttachment3b24b89b-fd14-49f9-ba12-3b4212b60080.txt");
    }

    @Test
    void When_BuildingFileNameIfTitleIsAbsent_Expect_FileNameIsGenerated() {
        var attachment = new Attachment()
            .setContentType(TEXT_PLAIN_CONTENT_TYPE);

        assertThat(DocumentReferenceUtils.buildAttachmentFileName(NARRATIVE_STATEMENT_ID, attachment))
            .isEqualTo("3b24b89b-fd14-49f9-ba12-3b4212b60080_3b24b89b-fd14-49f9-ba12-3b4212b60080.txt");
    }
}
