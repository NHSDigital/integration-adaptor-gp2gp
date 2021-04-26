package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpineInteraction {
    EHR_EXTRACT_REQUEST("RCMR_IN010000UK05"),
    CONTINUE_REQUEST("COPC_IN000001UK01"),
    ACKNOWLEDGMENT_REQUEST("MCCI_IN010000UK13");
    private final String interactionId;
}