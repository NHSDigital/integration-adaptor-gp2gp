package uk.nhs.adaptors.gp2gp.common.configuration;

public record RedactionsContext(String ehrExtractInteractionId) {
    public static final String NON_REDACTION_INTERACTION_ID = "RCMR_IN030000UK06";
    public static final String REDACTION_INTERACTION_ID = "RCMR_IN030000UK07";

    public boolean isRedactionMessage() {
        return REDACTION_INTERACTION_ID.equals(ehrExtractInteractionId);
    }
}