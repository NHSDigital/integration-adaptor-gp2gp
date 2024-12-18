package uk.nhs.adaptors.gp2gp.common.storage;

import java.io.InputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3StorageConnector implements StorageConnector {
    private final S3Client s3client;
    private final String bucketName;

    protected S3StorageConnector(StorageConnectorConfiguration configuration) {
        this.bucketName = configuration.getContainerName();
        this.s3client = S3Client.builder().build();
    }

    @Override
    public void uploadToStorage(InputStream is, long streamLength, String filename) throws StorageConnectorException {
        try {
            final var putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(filename).build();

            s3client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(is, streamLength)
            );
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred uploading to S3 Bucket", exception);
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            final var request = GetObjectRequest.builder().bucket(bucketName).key(filename).build();
            return s3client.getObject(request);
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred downloading from S3 Bucket", exception);
        }
    }
}
