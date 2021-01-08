package uk.nhs.adaptors.gp2gp.common.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(streamLength);

            PutObjectRequest putRequest = new PutObjectRequest(
                bucketName,
                filename,
                is,
                metadata);

            List<Tag> tags = new ArrayList<>();
            tags.add(new Tag("Tag 1", "This is tag 1"));
            tags.add(new Tag("Tag 2", "This is tag 2"));
            putRequest.setTagging(new ObjectTagging(tags));

            s3client.putObject(putRequest);

//            s3client.putObject(
//                    bucketName,
//                    filename,
//                    is,
//                    metadata
//            );
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred uploading to S3 Bucket", exception);
        }
    }

    @Override
    public InputStream downloadFromStorage(String filename) throws StorageConnectorException {
        try {
            S3Object s3Object = s3client.getObject(bucketName, filename);
            return s3Object.getObjectContent();
        } catch (Exception exception) {
            throw new StorageConnectorException("Error occurred downloading from S3 Bucket", exception);
        }
    }
}
