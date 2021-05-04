package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3StorageConnector implements StorageConnector {
    private final AmazonS3 s3client;
    private final String bucketName;

    protected S3StorageConnector(StorageConnectorConfiguration configuration) {
        this.bucketName = configuration.getContainerName();
        this.s3client = AmazonS3ClientBuilder
            .standard()
            .build();
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        try {
            LOGGER.info("S3_CONNECTOR uploadToStorage: " + filename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(streamLength);

            s3client.putObject(
                bucketName,
                filename,
                is,
                metadata
            );
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred uploading to S3 Bucket", exception);
        }
    }

    @Override
    public InputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            LOGGER.info("S3_CONNECTOR uploadToStorage: " + filename);

            S3Object s3Object = s3client.getObject(bucketName, filename);
            return s3Object.getObjectContent();
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred downloading from S3 Bucket", exception);
        }
    }
}
