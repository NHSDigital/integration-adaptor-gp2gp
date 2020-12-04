package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import lombok.Getter;

@Getter
public enum  StorageConnectorOptions {
    S3("S3"),
    AZURE("Azure"),
    LOCALMOCK("LocalMock");

    private String stringVal;
    StorageConnectorOptions(String stringVal) {
        this.stringVal = stringVal;
    }

    public static StorageConnectorOptions enumOf(String enumVal) {
        return valueOf(StorageConnectorOptions.class, enumVal.toUpperCase());
    }
}
