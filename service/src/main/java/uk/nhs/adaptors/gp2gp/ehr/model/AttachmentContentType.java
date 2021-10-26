package uk.nhs.adaptors.gp2gp.ehr.model;

public enum AttachmentContentType {

    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    APPLICATION_XML("application/xml"),
    TEXT_RTF("text/rtf"),
    AUDIO_BASIC("audio/basic"),
    AUDIO_MPEG("audio/mpeg"),
    IMAGE_PNG("image/png"),
    IMAGE_GIF("image/gif"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_TIFF("image/tiff"),
    VIDEO_MPEG("video/mpeg"),
    APPLICATION_MSWORD("application/msword"),
    APPLICATION_OCTET_STREAM("application/octet-stream");

    AttachmentContentType(String name) { }
}
