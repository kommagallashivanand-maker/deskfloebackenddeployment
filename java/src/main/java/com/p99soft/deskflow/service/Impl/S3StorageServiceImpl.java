package com.p99soft.deskflow.service.Impl;

import com.p99soft.deskflow.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = "uploads/" + UUID.randomUUID().toString() + fileExtension;
        log.info("Uploading file to S3: bucket={}, key={}, originalName={}", bucketName, key, originalFilename);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            log.info("File uploaded successfully. URL={}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new IOException("Failed to upload file to storage", e);
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/")) {
            return fileUrl;
        }

        // Only presign S3 URLs, return other formats as-is
        if (!fileUrl.contains(".amazonaws.com/")) {
            return fileUrl;
        }

        try {
            java.net.URI uri = new java.net.URI(fileUrl);
            String path = uri.getPath();
            // Remove the leading "/" to get the correct S3 key (e.g.
            // "uploads/filename.png")
            String objectKey = path.startsWith("/") ? path.substring(1) : path;

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60)) // Link is valid for 60 minutes
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
            return presignedGetObjectRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {}", fileUrl, e);
            return fileUrl;
        }
    }
}
