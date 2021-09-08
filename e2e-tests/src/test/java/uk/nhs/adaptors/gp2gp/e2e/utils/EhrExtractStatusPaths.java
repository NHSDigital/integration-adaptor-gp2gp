package uk.nhs.adaptors.gp2gp.e2e.utils;

public enum EhrExtractStatusPaths {

    // top level objects
    EHR_REQUEST("ehrRequest"),
    GPC_ACCESS_DOCUMENT("gpcAccessDocument"),
    GPC_ACCESS_STRUCTURED("gpcAccessStructured"),
    EHR_EXTRACT_CORE_PENDING("ehrExtractCorePending"),
    EHR_CONTINUE("ehrContinue"),
    EHR_EXTRACT_CORE("ehrExtractCore"),
    ACK_PENDING("ackPending"),
    ACK_TO_REQUESTER("ackToRequester"),
    EHR_RECEIVED_ACK("ehrReceivedAcknowledgement"),
    DOCUMENTS("documents"),
    MESSAGE_ID("messageId");

    private final String pathString;

    EhrExtractStatusPaths(String pathString) {
        this.pathString = pathString;
    }

    @Override
    public String toString() {
        return pathString;
    }
}
