package com.fitcoach.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploaded files in AWS S3 and returns the public HTTPS URL.
 * A local temp directory is kept for OCR processing (Tesseract needs a file path).
 */
@Service
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path localTempRoot;

    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() throws IOException {
        localTempRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(localTempRoot);
    }

    /**
     * Uploads {@code file} to S3 under key {@code subDir/uuid.ext} and returns
     * the public S3 HTTPS URL stored in the database.
     */
    public String store(MultipartFile file, String subDir) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }
        String key = subDir + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.debug("Uploaded to S3: s3://{}/{}", bucketName, key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    /**
     * Downloads an S3 object to a local temp file so Tesseract OCR can process it.
     * The caller is responsible for deleting the temp file after use.
     */
    public Path resolveStoredPath(String fileUrl) {
        if (fileUrl == null) throw new IllegalArgumentException("fileUrl is null");

        // Legacy local path support (files uploaded before S3 migration)
        if (fileUrl.startsWith("/uploads/")) {
            String relative = fileUrl.substring("/uploads/".length());
            return localTempRoot.resolve(relative).normalize();
        }

        // S3 URL — download to temp file for OCR
        String key = extractS3Key(fileUrl);
        try {
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            Path tempFile = Files.createTempFile("ocr-", "-" + fileName);

            s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(key).build(),
                tempFile
            );
            log.debug("Downloaded S3 object for OCR: {}", key);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download S3 file for OCR: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the object from S3 (or local disk for legacy paths).
     */
    public void delete(String fileUrl) {
        if (fileUrl == null) return;

        if (fileUrl.startsWith("/uploads/")) {
            // Legacy local file
            String relative = fileUrl.substring("/uploads/".length());
            Path filePath = localTempRoot.resolve(relative).normalize();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                log.warn("Could not delete local file {}: {}", filePath, ex.getMessage());
            }
            return;
        }

        // S3 URL
        String key = extractS3Key(fileUrl);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
            log.debug("Deleted from S3: {}", key);
        } catch (Exception ex) {
            log.warn("Could not delete S3 object {}: {}", key, ex.getMessage());
        }
    }

    private String extractS3Key(String s3Url) {
        // https://bucket.s3.region.amazonaws.com/key/path/file.ext
        int slashAfterHost = s3Url.indexOf('/', s3Url.indexOf("amazonaws.com/"));
        return s3Url.substring(slashAfterHost + 1);
    }
}
