package uk.nhs.adaptors.gp2gp.common.storageconnectors;

public class StorageConnectorException extends RuntimeException {

    public StorageConnectorException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
