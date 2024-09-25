package uk.nhs.adaptors.gp2gp.ehr.utils;

public enum ErrorConstants {

    ACK_TIMEOUT("99", "No acknowledgement has been received within ACK timeout limit");

    private final String code;
    private final String message;

    ErrorConstants(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
