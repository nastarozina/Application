package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.configuration.StorageProperties;
import ru.rozhi.exception.NoImageUploadedException;
import ru.rozhi.service.dto.UploadedImageInfo;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3StorageService implements ImageStorageService {

    @Autowired
    private S3Client client;

    @Autowired
    private S3Presigner presigner;

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public String createUploadUrl(String objectKey) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(storageProperties.getPresignedUrlExpiry())
                    .putObjectRequest(objectRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();

        } catch (Exception e) {
            throw new RuntimeException("Cannot create S3 presigned URL", e);
        }
    }

    @Override
    public UploadedImageInfo getImageInfo(String objectKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = client.headObject(request);

            return new UploadedImageInfo(
                    response.contentLength(),
                    response.contentType()
            );

        } catch (NoSuchKeyException e) {
            throw new NoImageUploadedException(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get image info for key: " + objectKey, e);
        }
    }

    @Override
    public void deleteImage(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .build();

            client.deleteObject(request);
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete object: " + objectKey, e);
        }
    }
}