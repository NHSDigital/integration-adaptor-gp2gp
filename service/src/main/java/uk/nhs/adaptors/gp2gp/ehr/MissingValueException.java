package uk.nhs.adaptors.gp2gp.ehr;

import lombok.Builder;

public class MissingValueException extends RuntimeException {
    @Builder
    protected MissingValueException(String xpath, SpineInteraction interaction) {
        super("The value at '" + xpath + "' is missing or blank in the Spine interaction " + interaction.getInteractionId());
    }
}
