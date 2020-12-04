package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import lombok.Getter;

@Getter
public enum  StorageConnectorOptions {
    S3("S3"),
    AZURE("Azure"),
    LOCALMOCK("LocalMock");

    private String stringValue;
    StorageConnectorOptions(String stringValue) {
        this.stringValue = stringValue;
    }

    public static StorageConnectorOptions enumOf(String enumValue) {
        return valueOf(StorageConnectorOptions.class, enumValue.toUpperCase());
    }
}
