package uk.nhs.adaptors.gp2gp.mhs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class OutboundMessage {
    private String payload;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Attachment> attachments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("external_attachments")
    private List<ExternalAttachment> externalAttachments;

    @Getter
    @Setter
    @Jacksonized
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class Attachment {
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("is_base64")
        private Boolean isBase64;
        private String description;
        private String payload;
    }

    @Getter
    @Setter
    @Jacksonized
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class ExternalAttachment {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("document_id")
        private String documentId;
        @JsonProperty("message_id")
        private String messageId;
        @JsonIgnore
        private String filename;
        private String description;
        @JsonIgnore
        private String title;
        @JsonIgnore
        private String url;
        @JsonIgnore
        private List<Identifier> identifier;
        @JsonIgnore
        private String originalDescription;
        @JsonIgnore
        private String contentType;
    }

    private static String booleanToYesNo(boolean value) {
        if (value) {
            return "Yes";
        } else {
            return "No";
        }
    }

    @Builder
    public static class AttachmentDescription {
        private static final String LENGTH_PLACEHOLDER = "LENGTH_PLACEHOLDER_ID";
        private static final String FILENAME_PLACEHOLDER = "FILENAME_PLACEHOLDER_ID";
        private static final String CONTENT_TYPE_PLACEHOLDER = "CONTENT_TYPE_PLACEHOLDER_ID";

        private final String fileName;
        private final String contentType;
        private final boolean compressed;
        private final boolean largeAttachment;
        private final boolean originalBase64;
        private final Integer length;
        private final String domainData;
        private final String documentId;

        @Override
        public String toString() {
            var descriptionElements = Stream.of(
                "Filename=\"" + (fileName == null ? generatePlaceholder(FILENAME_PLACEHOLDER) : fileName) + "\"",
                "ContentType=" + (contentType == null ? generatePlaceholder(CONTENT_TYPE_PLACEHOLDER) : contentType),
                "Compressed=" + booleanToYesNo(compressed),
                "LargeAttachment=" + booleanToYesNo(largeAttachment),
                "OriginalBase64=" + booleanToYesNo(originalBase64),
                Optional.ofNullable(length).map(value -> "Length=" + value).orElse(
                        Optional.ofNullable(documentId)
                        .map(docId -> "Length=" + generatePlaceholder(LENGTH_PLACEHOLDER))
                        .orElse(StringUtils.EMPTY)),
                Optional.ofNullable(domainData).map(value -> "DomainData=\"" + value + "\"").orElse(StringUtils.EMPTY));
            // all this below to pretty indent on MHS side
            return descriptionElements
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(StringUtils.SPACE));
        }

        private String generatePlaceholder(String placeholderName) {
            return "${" + placeholderName + "=" + documentId + "}";
        }
    }
}