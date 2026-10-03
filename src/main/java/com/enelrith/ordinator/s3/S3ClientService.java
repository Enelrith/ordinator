package com.enelrith.ordinator.s3;

import com.enelrith.ordinator.common.exception.S3PutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Service
public class S3ClientService {
    private static final Logger log = LoggerFactory.getLogger(S3ClientService.class);

    private final S3Client s3Client;

    @Value("${localstack.s3.bucketName}")
    private String bucketName;

    public S3ClientService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public PutObjectResponse putObject(String objectKey, MultipartFile file) {
        var request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        try (var inputStream = file.getInputStream()) {
            return s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            log.warn("Error while uploading file to S3: {}", e.getMessage());

            throw new S3PutException("Error while uploading attachment");
        }
    }

    public ResponseInputStream<GetObjectResponse> getObject(String objectKey) {
        var request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        return s3Client.getObject(request);
    }

    public void deleteObject(String objectKey) {
        var request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(request);
    }

    public byte[] getObjectAsByteArray(String objectKey) {
        var request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }
}
