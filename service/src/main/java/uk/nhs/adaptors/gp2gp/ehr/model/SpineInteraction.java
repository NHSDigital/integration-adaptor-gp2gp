package uk.nhs.adaptors.gp2gp.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpineInteraction {
    EHR_EXTRACT_REQUEST("RCMR_IN010000UK05");
    private final String interactionId;
}