package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

public class S3StorageConnector implements StorageConnector {

    private final AmazonS3 s3client;
    private final String bucketName;

    protected S3StorageConnector(StorageConnectorConfiguration configuration) {
        this.bucketName = configuration.getContainerName();
        AWSCredentials credentials = new BasicAWSCredentials(
            configuration.getS3AccessKey(),
            configuration.getS3SecretKey()
        );
        this.s3client = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion(Regions.EU_WEST_2)
            .build();
    }

    @Override
    public void uploadToStorage(InputStream is, String filename) throws StorageConnectorException {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(is.available());
            s3client.putObject(
                bucketName,
                filename,
                is,
                metadata
            );
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred uploading to S3 Bucket", ioException);
        }
    }

    @Override
    public OutputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            S3Object s3Object = s3client.getObject(bucketName, filename);
            InputStream in = s3Object.getObjectContent();
            OutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out;
        } catch (IOException ioException) {
            throw new StorageConnectorException("Error occurred downloading from S3 Bucket", ioException);
        }
    }
}
