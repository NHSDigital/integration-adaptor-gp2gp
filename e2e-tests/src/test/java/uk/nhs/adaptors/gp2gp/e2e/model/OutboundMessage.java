package uk.nhs.adaptors.gp2gp.e2e.model;

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

import java.util.List;

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
        private String isBase64;
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
        @JsonProperty("document_id")
        private String documentId;
        @JsonProperty("message_id")
        private String messageId;
        @JsonIgnore
        private String filename;
        private String description;
        @JsonIgnore
        private String url;
        @JsonIgnore
        private List<EhrStatus.AttachmentStatus.Identifier> identifier;
        @JsonIgnore
        private String originalDescription;
    }
}