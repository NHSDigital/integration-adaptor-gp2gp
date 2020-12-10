package uk.nhs.adaptors.gp2gp.common.storage;

public class StorageConnectorException extends RuntimeException {
    public StorageConnectorException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
