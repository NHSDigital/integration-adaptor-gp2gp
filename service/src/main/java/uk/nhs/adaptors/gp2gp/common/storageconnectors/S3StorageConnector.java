package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3StorageConnector implements StorageConnector {

    private final AmazonS3 s3client;
    private static final String BUCKET_NAME = "for-nia-testing";

    protected S3StorageConnector() {
        this.s3client = AmazonS3ClientBuilder
            .standard()
            .build();
    }

    @Override
    public void uploadToStorage(InputStream is, String filename) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(is.available());

        s3client.putObject(
            BUCKET_NAME,
            filename,
            is,
            metadata
        );
    }

    @Override
    public OutputStream downloadFromStorage(String filename) throws IOException {
        S3Object s3Object = s3client.getObject(BUCKET_NAME, filename);

        InputStream in = s3Object.getObjectContent();
        OutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);

        return out;
    }

    @Override
    public List<String> getFileListFromStorage() {
        ObjectListing objects = s3client.listObjects(BUCKET_NAME);
        List<S3ObjectSummary> objectSummaries = objects.getObjectSummaries();

        return objectSummaries.stream()
            .map(S3ObjectSummary::getKey)
            .collect(Collectors.toList());
    }
}
