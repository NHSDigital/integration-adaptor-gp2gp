package uk.nhs.adaptors.gp2gp.ehr.utils;

import lombok.Getter;

@Getter
public enum ErrorDetail {

    ACK_TIMEOUT("99", "No acknowledgement has been received within ACK timeout limit");

    private final String code;
    private final String message;

    ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
